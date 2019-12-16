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

import org.argeo.cms.auth.CmsSessionId;
import org.argeo.cms.auth.CurrentUser;
import org.argeo.cms.auth.HttpRequestCallback;
import org.argeo.cms.auth.HttpRequestCallbackHandler;
import org.argeo.node.NodeConstants;

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
		LoginContext lc = null;
		try {
			lc = new LoginContext(NodeConstants.LOGIN_CONTEXT_USER, new HttpRequestCallbackHandler(request, response) {
				public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
					for (Callback callback : callbacks) {
						if (callback instanceof HttpRequestCallback) {
							((HttpRequestCallback) callback).setRequest(request);
							((HttpRequestCallback) callback).setResponse(response);
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
