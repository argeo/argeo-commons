package org.argeo.cms.integration;

import java.io.IOException;
import java.util.Set;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.argeo.api.cms.CmsAuth;
import org.argeo.api.cms.CmsSessionId;
import org.argeo.cms.CurrentUser;
import org.argeo.cms.auth.RemoteAuthCallback;
import org.argeo.cms.auth.RemoteAuthCallbackHandler;
import org.argeo.cms.servlet.ServletHttpRequest;
import org.argeo.cms.servlet.ServletHttpResponse;

/** Externally authenticate an http session. */
public class CmsLogoutServlet extends HttpServlet {
	private static final long serialVersionUID = 2478080654328751539L;

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		doPost(request, response);
	}

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		ServletHttpRequest httpRequest = new ServletHttpRequest(request);
		ServletHttpResponse httpResponse = new ServletHttpResponse(response);
		LoginContext lc = null;
		try {
			lc = new LoginContext(CmsAuth.LOGIN_CONTEXT_USER,
					new RemoteAuthCallbackHandler(httpRequest, httpResponse) {
						public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
							for (Callback callback : callbacks) {
								if (callback instanceof RemoteAuthCallback) {
									((RemoteAuthCallback) callback).setRequest(httpRequest);
									((RemoteAuthCallback) callback).setResponse(httpResponse);
								}
							}
						}
					});
			lc.login();

			Subject subject = lc.getSubject();
			CmsSessionId cmsSessionId = extractFrom(subject.getPrivateCredentials(CmsSessionId.class));
			if (cmsSessionId != null) {// logged in
				CurrentUser.logoutCmsSession(subject);
			}

		} catch (LoginException e) {
			// ignore
		}

		String redirectTo = redirectTo(request);
		if (redirectTo != null)
			response.sendRedirect(redirectTo);
	}

	protected <T> T extractFrom(Set<T> creds) {
		if (creds.size() > 0)
			return creds.iterator().next();
		else
			return null;
	}

	protected String redirectTo(HttpServletRequest request) {
		return null;
	}
}
