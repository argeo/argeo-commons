package org.argeo.cms.jakarta.servlet;

import java.io.IOException;
import java.net.URL;
import java.util.Map;

import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import org.argeo.api.cms.CmsAuth;
import org.argeo.api.cms.CmsLog;
import org.argeo.cms.auth.RemoteAuthCallbackHandler;
import org.argeo.cms.auth.RemoteAuthRequest;
import org.argeo.cms.auth.RemoteAuthResponse;
import org.argeo.cms.auth.RemoteAuthUtils;
import org.argeo.internal.cms.jakarta.HttpUtils;
import org.eclipse.rap.service.http.HttpContext;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.osgi.service.http.context.ServletContextHelper;

/**
 * Default servlet context degrading to anonymous if the the session is not
 * pre-authenticated.
 */
public class CmsServletContext extends ServletContextHelper {
	private final static CmsLog log = CmsLog.getLog(CmsServletContext.class);
	// use CMS bundle for resources
	private Bundle bundle = FrameworkUtil.getBundle(getClass());

	private final String httpAuthRealm = "Argeo";
	private final boolean forceBasic = false;

	public void init(Map<String, String> properties) {

	}

	public void destroy() {

	}

//	@Override
	public boolean handleSecurity(HttpServletRequest request, HttpServletResponse response) throws IOException {
		if (log.isTraceEnabled())
			HttpUtils.logRequestHeaders(log, request);
		RemoteAuthRequest remoteAuthRequest = new ServletHttpRequest(request);
		RemoteAuthResponse remoteAuthResponse = new ServletHttpResponse(response);
		ClassLoader currentThreadContextClassLoader = Thread.currentThread().getContextClassLoader();
		Thread.currentThread().setContextClassLoader(CmsServletContext.class.getClassLoader());
		LoginContext lc;
		try {
			lc = CmsAuth.USER.newLoginContext(new RemoteAuthCallbackHandler(remoteAuthRequest, remoteAuthResponse));
			lc.login();
		} catch (LoginException e) {
			if (authIsRequired(remoteAuthRequest, remoteAuthResponse)) {
				int statusCode = RemoteAuthUtils.askForWwwAuth(remoteAuthRequest,
						remoteAuthResponse, httpAuthRealm,
						forceBasic);
				response.setStatus(statusCode);
				return false;

			} else {
				lc = RemoteAuthUtils.anonymousLogin(remoteAuthRequest, remoteAuthResponse);
			}
			if (lc == null)
				return false;
		} finally {
			Thread.currentThread().setContextClassLoader(currentThreadContextClassLoader);
		}

//		Subject subject = lc.getSubject();
//		Subject.doAs(subject, new PrivilegedAction<Void>() {
//
//			@Override
//			public Void run() {
//				// TODO also set login context in order to log out ?
//				RemoteAuthUtils.configureRequestSecurity(remoteAuthRequest);
//				return null;
//			}
//
//		});
		return true;
	}

//	@Override
//	public void finishSecurity(HttpServletRequest request, HttpServletResponse response) {
//		RemoteAuthUtils.clearRequestSecurity(new ServletHttpRequest(request));
//	}

	protected boolean authIsRequired(RemoteAuthRequest remoteAuthRequest, RemoteAuthResponse remoteAuthResponse) {
		return false;
	}

//	protected LoginContext processUnauthorized(HttpServletRequest request, HttpServletResponse response) {
//		// anonymous
//		ClassLoader currentContextClassLoader = Thread.currentThread().getContextClassLoader();
//		try {
//			Thread.currentThread().setContextClassLoader(CmsServletContext.class.getClassLoader());
//			LoginContext lc = CmsAuth.ANONYMOUS.newLoginContext(
//					new RemoteAuthCallbackHandler(new ServletHttpRequest(request), new ServletHttpResponse(response)));
//			lc.login();
//			return lc;
//		} catch (LoginException e1) {
//			if (log.isDebugEnabled())
//				log.error("Cannot log in as anonymous", e1);
//			return null;
//		} finally {
//			Thread.currentThread().setContextClassLoader(currentContextClassLoader);
//		}
//	}

	@Override
	public URL getResource(String name) {
		// TODO make it more robust and versatile
		// if used directly it can only load from within this bundle
		return bundle.getResource(name);
	}

	@Override
	public String getMimeType(String name) {
		return null;
	}

	
}
