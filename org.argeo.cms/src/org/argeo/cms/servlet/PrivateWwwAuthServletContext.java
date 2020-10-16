package org.argeo.cms.servlet;

import javax.security.auth.login.LoginContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.argeo.cms.internal.http.HttpUtils;

/** Servlet context forcing authentication. */
public class PrivateWwwAuthServletContext extends CmsServletContext {
	// TODO make it configurable
	private final String httpAuthRealm = "Argeo";
	private final boolean forceBasic = false;

	@Override
	protected LoginContext processUnauthorized(HttpServletRequest request, HttpServletResponse response) {
		askForWwwAuth(request, response);
		return null;
	}

	protected void askForWwwAuth(HttpServletRequest request, HttpServletResponse response) {
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
		response.setStatus(401);
	}
}
