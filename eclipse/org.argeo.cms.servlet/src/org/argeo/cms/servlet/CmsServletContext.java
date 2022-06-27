package org.argeo.cms.servlet;

import java.io.IOException;
import java.net.URL;
import java.security.PrivilegedAction;
import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.argeo.api.cms.CmsAuth;
import org.argeo.api.cms.CmsLog;
import org.argeo.cms.auth.RemoteAuthCallbackHandler;
import org.argeo.cms.auth.RemoteAuthUtils;
import org.argeo.cms.servlet.internal.HttpUtils;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.http.context.ServletContextHelper;

/**
 * Default servlet context degrading to anonymous if the the session is not
 * pre-authenticated.
 */
public class CmsServletContext extends ServletContextHelper {
	private final static CmsLog log = CmsLog.getLog(CmsServletContext.class);
	// use CMS bundle for resources
	private Bundle bundle = FrameworkUtil.getBundle(getClass());

	public void init(Map<String, String> properties) {

	}

	public void destroy() {

	}

	@Override
	public boolean handleSecurity(HttpServletRequest request, HttpServletResponse response) throws IOException {
		if (log.isTraceEnabled())
			HttpUtils.logRequestHeaders(log, request);
		LoginContext lc;
		try {
			lc = CmsAuth.USER.newLoginContext(
					new RemoteAuthCallbackHandler(new ServletHttpRequest(request), new ServletHttpResponse(response)));
			lc.login();
		} catch (LoginException e) {
			lc = processUnauthorized(request, response);
			if (log.isTraceEnabled())
				HttpUtils.logResponseHeaders(log, response);
			if (lc == null)
				return false;
		}

		Subject subject = lc.getSubject();
		// log.debug("SERVLET CONTEXT: "+subject);
		Subject.doAs(subject, new PrivilegedAction<Void>() {

			@Override
			public Void run() {
				// TODO also set login context in order to log out ?
				RemoteAuthUtils.configureRequestSecurity(new ServletHttpRequest(request));
				return null;
			}

		});
		return true;
	}

	@Override
	public void finishSecurity(HttpServletRequest request, HttpServletResponse response) {
		RemoteAuthUtils.clearRequestSecurity(new ServletHttpRequest(request));
	}

	protected LoginContext processUnauthorized(HttpServletRequest request, HttpServletResponse response) {
		// anonymous
		ClassLoader currentContextClassLoader = Thread.currentThread().getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader(CmsServletContext.class.getClassLoader());
			LoginContext lc = new LoginContext(CmsAuth.LOGIN_CONTEXT_ANONYMOUS,
					new RemoteAuthCallbackHandler(new ServletHttpRequest(request), new ServletHttpResponse(response)));
			lc.login();
			return lc;
		} catch (LoginException e1) {
			if (log.isDebugEnabled())
				log.error("Cannot log in as anonymous", e1);
			return null;
		} finally {
			Thread.currentThread().setContextClassLoader(currentContextClassLoader);
		}
	}

	@Override
	public URL getResource(String name) {
		// TODO make it more robust and versatile
		// if used directly it can only load from within this bundle
		return bundle.getResource(name);
	}

}
