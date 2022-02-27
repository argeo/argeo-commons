package org.argeo.cms.auth;

import javax.security.auth.callback.Callback;

/** Retrieves credentials from an HTTP request. */
public class RemoteAuthCallback implements Callback {
	private RemoteAuthRequest request;
	private RemoteAuthResponse response;
	private RemoteAuthSession httpSession;

	public RemoteAuthRequest getRequest() {
		return request;
	}

	public void setRequest(RemoteAuthRequest request) {
		this.request = request;
	}

	public RemoteAuthResponse getResponse() {
		return response;
	}

	public void setResponse(RemoteAuthResponse response) {
		this.response = response;
	}

	public RemoteAuthSession getHttpSession() {
		return httpSession;
	}

	public void setHttpSession(RemoteAuthSession httpSession) {
		this.httpSession = httpSession;
	}

}
