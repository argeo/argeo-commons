package org.argeo.cms.javax.servlet;

import java.io.IOException;

import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.argeo.api.cms.CmsAuth;
import org.argeo.cms.auth.RemoteAuthCallbackHandler;
import org.argeo.cms.auth.RemoteAuthRequest;
import org.argeo.cms.auth.RemoteAuthResponse;
import org.argeo.cms.auth.RemoteAuthUtils;

/**
 * Authenticating filter degrading to anonymous if the the session is not
 * pre-authenticated.
 */
public class JavaxAuthFilter extends HttpFilter {

	private static final long serialVersionUID = -7536266717807144843L;

	private String httpAuthRealm = "Argeo";
	private boolean forceBasic = false;
	private boolean authRequired = true;

	@Override
	protected void doFilter(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws IOException, ServletException {
		RemoteAuthRequest remoteAuthRequest = new JavaxServletHttpRequest(request);
		RemoteAuthResponse remoteAuthResponse = new JavaxServletHttpResponse(response);
		ClassLoader currentThreadContextClassLoader = Thread.currentThread().getContextClassLoader();
		Thread.currentThread().setContextClassLoader(JavaxAuthFilter.class.getClassLoader());
		LoginContext lc;
		try {
			lc = CmsAuth.USER.newLoginContext(new RemoteAuthCallbackHandler(remoteAuthRequest, remoteAuthResponse));
			lc.login();
		} catch (LoginException e) {
			if (authIsRequired(remoteAuthRequest, remoteAuthResponse)) {
				int statusCode = RemoteAuthUtils.askForWwwAuth(remoteAuthRequest, remoteAuthResponse, httpAuthRealm,
						forceBasic);
				response.setStatus(statusCode);
				return;

			} else {
				lc = RemoteAuthUtils.anonymousLogin(remoteAuthRequest, remoteAuthResponse);
			}
			if (lc == null)
				return;
		} finally {
			Thread.currentThread().setContextClassLoader(currentThreadContextClassLoader);
		}

		chain.doFilter(request, response);
	}

	protected boolean authIsRequired(RemoteAuthRequest remoteAuthRequest, RemoteAuthResponse remoteAuthResponse) {
		return authRequired;
	}

	public boolean isAuthRequired() {
		return authRequired;
	}

	// FIXME make it safer
	public void setAuthRequired(boolean authRequired) {
		this.authRequired = authRequired;
	}
	
	
}
