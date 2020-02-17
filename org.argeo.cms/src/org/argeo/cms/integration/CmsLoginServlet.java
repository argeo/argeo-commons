package org.argeo.cms.integration;

import java.io.IOException;
import java.util.Locale;
import java.util.Set;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.argeo.cms.auth.CmsSessionId;
import org.argeo.cms.auth.HttpRequestCallback;
import org.argeo.cms.auth.HttpRequestCallbackHandler;
import org.argeo.node.NodeConstants;
import org.osgi.service.useradmin.Authorization;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;

/** Externally authenticate an http session. */
public class CmsLoginServlet extends HttpServlet {
	public final static String PARAM_USERNAME = "username";
	public final static String PARAM_PASSWORD = "password";

	private static final long serialVersionUID = 2478080654328751539L;
	private ObjectMapper objectMapper = new ObjectMapper();

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		doPost(request, response);
	}

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		LoginContext lc = null;
		String username = request.getParameter(PARAM_USERNAME);
		String password = request.getParameter(PARAM_PASSWORD);
		try {
			lc = new LoginContext(NodeConstants.LOGIN_CONTEXT_USER, new HttpRequestCallbackHandler(request, response) {
				public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
					for (Callback callback : callbacks) {
						if (callback instanceof NameCallback && username != null)
							((NameCallback) callback).setName(username);
						else if (callback instanceof PasswordCallback && password != null)
							((PasswordCallback) callback).setPassword(password.toCharArray());
						else if (callback instanceof HttpRequestCallback) {
							((HttpRequestCallback) callback).setRequest(request);
							((HttpRequestCallback) callback).setResponse(response);
						}
					}
				}
			});
			lc.login();

			Subject subject = lc.getSubject();
			CmsSessionId cmsSessionId = extractFrom(subject.getPrivateCredentials(CmsSessionId.class));
			if (cmsSessionId == null) {
				response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
				return;
			}
			Authorization authorization = extractFrom(subject.getPrivateCredentials(Authorization.class));
			Locale locale = extractFrom(subject.getPublicCredentials(Locale.class));

			CmsSessionDescriptor cmsSessionDescriptor = new CmsSessionDescriptor(authorization.getName(),
					cmsSessionId.getUuid().toString(), authorization.getRoles(), authorization.toString(),
					locale != null ? locale.toString() : null);

			response.setContentType("application/json");
			JsonGenerator jg = objectMapper.getFactory().createGenerator(response.getWriter());
			jg.writeObject(cmsSessionDescriptor);

			String redirectTo = redirectTo(request);
			if (redirectTo != null)
				response.sendRedirect(redirectTo);
		} catch (LoginException e) {
			response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
			return;
		}
	}

	protected <T> T extractFrom(Set<T> creds) {
		if (creds.size() > 0)
			return creds.iterator().next();
		else
			return null;
	}

	/**
	 * To be overridden in order to return a richer {@link CmsSessionDescriptor} to
	 * be serialized.
	 */
	protected CmsSessionDescriptor enrichJson(CmsSessionDescriptor cmsSessionDescriptor) {
		return cmsSessionDescriptor;
	}

	protected String redirectTo(HttpServletRequest request) {
		return null;
	}
}
