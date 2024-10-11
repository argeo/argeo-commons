package org.argeo.cms.jakarta.servlet;

import java.util.Objects;

import jakarta.servlet.http.HttpServletResponse;

import org.argeo.cms.auth.RemoteAuthResponse;

public class ServletHttpResponse implements RemoteAuthResponse {
	private final HttpServletResponse response;

	public ServletHttpResponse(HttpServletResponse response) {
		Objects.requireNonNull(response);
		this.response = response;
	}

	@Override
	public void setHeader(String headerName, String value) {
		response.setHeader(headerName, value);
	}

	@Override
	public void addHeader(String headerName, String value) {
		response.addHeader(headerName, value);
	}

}
