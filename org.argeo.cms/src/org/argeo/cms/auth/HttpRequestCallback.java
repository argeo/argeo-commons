package org.argeo.cms.auth;

import javax.security.auth.callback.Callback;

/** Retrieves credentials from an HTTP request. */
public class HttpRequestCallback implements Callback {
	private HttpRequest request;
	private HttpResponse response;
	private HttpSession httpSession;

	public HttpRequest getRequest() {
		return request;
	}

	public void setRequest(HttpRequest request) {
		this.request = request;
	}

	public HttpResponse getResponse() {
		return response;
	}

	public void setResponse(HttpResponse response) {
		this.response = response;
	}

	public HttpSession getHttpSession() {
		return httpSession;
	}

	public void setHttpSession(HttpSession httpSession) {
		this.httpSession = httpSession;
	}

}
