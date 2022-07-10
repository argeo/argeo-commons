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

import org.argeo.api.cms.CmsAuth;
import org.argeo.api.cms.CmsSessionId;
import org.argeo.cms.auth.RemoteAuthCallback;
import org.argeo.cms.auth.RemoteAuthCallbackHandler;
import org.argeo.cms.servlet.ServletHttpRequest;
import org.argeo.cms.servlet.ServletHttpResponse;
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
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		LoginContext lc = null;
		String username = req.getParameter(PARAM_USERNAME);
		String password = req.getParameter(PARAM_PASSWORD);
		ServletHttpRequest request = new ServletHttpRequest(req);
		ServletHttpResponse response = new ServletHttpResponse(resp);
		try {
			lc = new LoginContext(CmsAuth.LOGIN_CONTEXT_USER, new RemoteAuthCallbackHandler(request, response) {
				public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
					for (Callback callback : callbacks) {
						if (callback instanceof NameCallback && username != null)
							((NameCallback) callback).setName(username);
						else if (callback instanceof PasswordCallback && password != null)
							((PasswordCallback) callback).setPassword(password.toCharArray());
						else if (callback instanceof RemoteAuthCallback) {
							((RemoteAuthCallback) callback).setRequest(request);
							((RemoteAuthCallback) callback).setResponse(response);
						}
					}
				}
			});
			lc.login();

			Subject subject = lc.getSubject();
			CmsSessionId cmsSessionId = extractFrom(subject.getPrivateCredentials(CmsSessionId.class));
			if (cmsSessionId == null) {
				resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
				return;
			}
			Authorization authorization = extractFrom(subject.getPrivateCredentials(Authorization.class));
			Locale locale = extractFrom(subject.getPublicCredentials(Locale.class));

			CmsSessionDescriptor cmsSessionDescriptor = new CmsSessionDescriptor(authorization.getName(),
					cmsSessionId.getUuid().toString(), authorization.getRoles(), authorization.toString(),
					locale != null ? locale.toString() : null);

			resp.setContentType("application/json");
			JsonGenerator jg = objectMapper.getFactory().createGenerator(resp.getWriter());
			jg.writeObject(cmsSessionDescriptor);

			String redirectTo = redirectTo(req);
			if (redirectTo != null)
				resp.sendRedirect(redirectTo);
		} catch (LoginException e) {
			resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
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
