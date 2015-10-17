package org.argeo.cms.auth;

import javax.security.auth.callback.Callback;
import javax.servlet.http.HttpServletRequest;

public class HttpRequestCallback implements Callback {
	private HttpServletRequest request;

	public HttpServletRequest getRequest() {
		return request;
	}

	public void setRequest(HttpServletRequest request) {
		this.request = request;
	}
	// private X509Certificate extractCertificate(HttpServletRequest req) {
	// X509Certificate[] certs = (X509Certificate[]) req
	// .getAttribute("javax.servlet.request.X509Certificate");
	// if (null != certs && certs.length > 0) {
	// return certs[0];
	// }
	// return null;
	// }

}
