package org.argeo.cms.integration;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.Set;
import java.util.UUID;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.argeo.api.acr.ldap.NamingUtils;
import org.argeo.api.cms.CmsAuth;
import org.argeo.cms.CmsUserManager;
import org.argeo.cms.auth.RemoteAuthCallback;
import org.argeo.cms.auth.RemoteAuthCallbackHandler;
import org.argeo.cms.servlet.ServletHttpRequest;
import org.argeo.cms.servlet.ServletHttpResponse;
import org.osgi.service.useradmin.Authorization;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;

/** Provides access to tokens. */
public class CmsTokenServlet extends HttpServlet {
	private static final long serialVersionUID = 302918711430864140L;

	public final static String PARAM_EXPIRY_DATE = "expiryDate";
	public final static String PARAM_TOKEN = "token";

	private final static int DEFAULT_HOURS = 24;

	private CmsUserManager userManager;
	private ObjectMapper objectMapper = new ObjectMapper();

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		ServletHttpRequest request = new ServletHttpRequest(req);
		ServletHttpResponse response = new ServletHttpResponse(resp);
		LoginContext lc = null;
		try {
			lc = new LoginContext(CmsAuth.LOGIN_CONTEXT_USER, new RemoteAuthCallbackHandler(request, response) {
				public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
					for (Callback callback : callbacks) {
						if (callback instanceof RemoteAuthCallback) {
							((RemoteAuthCallback) callback).setRequest(request);
							((RemoteAuthCallback) callback).setResponse(response);
						}
					}
				}
			});
			lc.login();
		} catch (LoginException e) {
			// ignore
		}

		try {
			Subject subject = lc.getSubject();
			Authorization authorization = extractFrom(subject.getPrivateCredentials(Authorization.class));
			String token = UUID.randomUUID().toString();
			String expiryDateStr = req.getParameter(PARAM_EXPIRY_DATE);
			ZonedDateTime expiryDate;
			if (expiryDateStr != null) {
				expiryDate = NamingUtils.ldapDateToZonedDateTime(expiryDateStr);
			} else {
				expiryDate = ZonedDateTime.now().plusHours(DEFAULT_HOURS);
				expiryDateStr = NamingUtils.instantToLdapDate(expiryDate);
			}
			userManager.addAuthToken(authorization.getName(), token, expiryDate);

			TokenDescriptor tokenDescriptor = new TokenDescriptor();
			tokenDescriptor.setUsername(authorization.getName());
			tokenDescriptor.setToken(token);
			tokenDescriptor.setExpiryDate(expiryDateStr);
//			tokenDescriptor.setRoles(Collections.unmodifiableSortedSet(new TreeSet<>(Arrays.asList(roles))));

			resp.setContentType("application/json");
			JsonGenerator jg = objectMapper.getFactory().createGenerator(resp.getWriter());
			jg.writeObject(tokenDescriptor);
		} catch (Exception e) {
			new CmsExceptionsChain(e).writeAsJson(objectMapper, resp);
		}
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		// temporarily wrap POST for ease of testing
		doPost(req, resp);
	}

	@Override
	protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		try {
			String token = req.getParameter(PARAM_TOKEN);
			userManager.expireAuthToken(token);
		} catch (Exception e) {
			new CmsExceptionsChain(e).writeAsJson(objectMapper, resp);
		}
	}

	protected <T> T extractFrom(Set<T> creds) {
		if (creds.size() > 0)
			return creds.iterator().next();
		else
			return null;
	}

	public void setUserManager(CmsUserManager userManager) {
		this.userManager = userManager;
	}
}
