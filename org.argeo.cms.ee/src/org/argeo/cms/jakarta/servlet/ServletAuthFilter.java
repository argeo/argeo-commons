package org.argeo.cms.jakarta.servlet;

import java.io.IOException;

import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import org.argeo.api.cms.CmsAuth;
import org.argeo.cms.auth.RemoteAuthCallbackHandler;
import org.argeo.cms.auth.RemoteAuthRequest;
import org.argeo.cms.auth.RemoteAuthResponse;
import org.argeo.cms.auth.RemoteAuthUtils;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Default servlet context degrading to anonymous if the the session is not
 * pre-authenticated.
 */
public class ServletAuthFilter extends HttpFilter {

	private static final long serialVersionUID = 8916866306520391671L;
	private String httpAuthRealm = "Argeo";
	private boolean forceBasic = false;
	private boolean authRequired = true;

	@Override
	protected void doFilter(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws IOException, ServletException {
		RemoteAuthRequest remoteAuthRequest = new ServletHttpRequest(request);
		RemoteAuthResponse remoteAuthResponse = new ServletHttpResponse(response);
		// TODO factorize
		ClassLoader currentThreadContextClassLoader = Thread.currentThread().getContextClassLoader();
		Thread.currentThread().setContextClassLoader(ServletAuthFilter.class.getClassLoader());
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
