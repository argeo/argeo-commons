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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.argeo.api.NodeConstants;
import org.argeo.cms.auth.HttpRequestCallbackHandler;
import org.argeo.cms.auth.ServletAuthUtils;
import org.argeo.cms.internal.http.HttpUtils;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.http.context.ServletContextHelper;

/**
 * Default servlet context degrading to anonymous if the the sesison is not
 * pre-authenticated.
 */
public class CmsServletContext extends ServletContextHelper {
	private final static Log log = LogFactory.getLog(CmsServletContext.class);
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
			lc = new LoginContext(NodeConstants.LOGIN_CONTEXT_USER, new HttpRequestCallbackHandler(request, response));
			lc.login();
		} catch (LoginException e) {
			lc = processUnauthorized(request, response);
			if (log.isTraceEnabled())
				HttpUtils.logResponseHeaders(log, response);
			if (lc == null)
				return false;
		}
		
		Subject subject = lc.getSubject();
		//log.debug("SERVLET CONTEXT: "+subject);
		Subject.doAs(subject, new PrivilegedAction<Void>() {

			@Override
			public Void run() {
				// TODO also set login context in order to log out ?
				ServletAuthUtils.configureRequestSecurity(request);
				return null;
			}

		});
		return true;
	}

	@Override
	public void finishSecurity(HttpServletRequest request, HttpServletResponse response) {
		ServletAuthUtils.clearRequestSecurity(request);
	}

	protected LoginContext processUnauthorized(HttpServletRequest request, HttpServletResponse response) {
		// anonymous
		try {
			LoginContext lc = new LoginContext(NodeConstants.LOGIN_CONTEXT_ANONYMOUS,
					new HttpRequestCallbackHandler(request, response));
			lc.login();
			return lc;
		} catch (LoginException e1) {
			if (log.isDebugEnabled())
				log.error("Cannot log in as anonymous", e1);
			return null;
		}
	}

	@Override
	public URL getResource(String name) {
		return bundle.getResource(name);
	}

}
