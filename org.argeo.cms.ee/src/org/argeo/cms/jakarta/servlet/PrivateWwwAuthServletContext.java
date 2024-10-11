package org.argeo.cms.jakarta.servlet;

import org.argeo.cms.auth.RemoteAuthRequest;
import org.argeo.cms.auth.RemoteAuthResponse;

/** Servlet context forcing authentication. */
public class PrivateWwwAuthServletContext extends CmsServletContext {
	// TODO make it configurable
//	private final String httpAuthRealm = "Argeo";
//	private final boolean forceBasic = false;
	
	protected boolean authIsRequired(RemoteAuthRequest remoteAuthRequest,
			RemoteAuthResponse remoteAuthResponse) {
		return true;
	}


//	@Override
//	protected LoginContext processUnauthorized(HttpServletRequest request, HttpServletResponse response) {
//		askForWwwAuth(request, response);
//		return null;
//	}
//
//	protected void askForWwwAuth(HttpServletRequest request, HttpServletResponse response) {
//		// response.setHeader(HttpUtils.HEADER_WWW_AUTHENTICATE, "basic
//		// realm=\"" + httpAuthRealm + "\"");
//		if (SpnegoLoginModule.hasAcceptorCredentials() && !forceBasic)// SPNEGO
//			response.setHeader(HttpHeader.WWW_AUTHENTICATE.getName(), HttpHeader.NEGOTIATE);
//		else
//			response.setHeader(HttpHeader.WWW_AUTHENTICATE.getName(),
//					HttpHeader.BASIC + " " + HttpHeader.REALM + "=\"" + httpAuthRealm + "\"");
//
//		// response.setDateHeader("Date", System.currentTimeMillis());
//		// response.setDateHeader("Expires", System.currentTimeMillis() + (24 *
//		// 60 * 60 * 1000));
//		// response.setHeader("Accept-Ranges", "bytes");
//		// response.setHeader("Connection", "Keep-Alive");
//		// response.setHeader("Keep-Alive", "timeout=5, max=97");
//		// response.setContentType("text/html; charset=UTF-8");
//		response.setStatus(401);
//	}
}
