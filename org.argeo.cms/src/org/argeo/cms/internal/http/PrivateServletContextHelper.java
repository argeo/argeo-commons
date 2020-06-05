package org.argeo.cms.internal.http;

import java.io.IOException;
import java.net.URL;
import java.util.Map;

import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.argeo.api.NodeConstants;
import org.argeo.cms.auth.HttpRequestCallbackHandler;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.http.context.ServletContextHelper;

public class PrivateServletContextHelper extends ServletContextHelper {
	private final static Log log = LogFactory.getLog(PrivateServletContextHelper.class);

	// TODO make it configurable
	private final String httpAuthRealm = "Argeo";
	private final boolean forceBasic = false;

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
			askForWwwAuth(request, response);
			return false;
		}
		return true;
	}

	protected void askForWwwAuth(HttpServletRequest request, HttpServletResponse response) {
		response.setStatus(401);
		// response.setHeader(HttpUtils.HEADER_WWW_AUTHENTICATE, "basic
		// realm=\"" + httpAuthRealm + "\"");
		if (org.argeo.cms.internal.kernel.Activator.getAcceptorCredentials() != null && !forceBasic)// SPNEGO
			response.setHeader(HttpUtils.HEADER_WWW_AUTHENTICATE, "Negotiate");
		else
			response.setHeader(HttpUtils.HEADER_WWW_AUTHENTICATE, "Basic realm=\"" + httpAuthRealm + "\"");

		// response.setDateHeader("Date", System.currentTimeMillis());
		// response.setDateHeader("Expires", System.currentTimeMillis() + (24 *
		// 60 * 60 * 1000));
		// response.setHeader("Accept-Ranges", "bytes");
		// response.setHeader("Connection", "Keep-Alive");
		// response.setHeader("Keep-Alive", "timeout=5, max=97");
		// response.setContentType("text/html; charset=UTF-8");

	}

	@Override
	public URL getResource(String name) {
		return bundle.getResource(name);
	}

}
