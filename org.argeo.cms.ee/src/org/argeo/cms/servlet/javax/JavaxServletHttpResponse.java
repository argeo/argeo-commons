package org.argeo.cms.servlet.javax;

import java.util.Objects;

import javax.servlet.http.HttpServletResponse;

import org.argeo.cms.auth.RemoteAuthResponse;

public class JavaxServletHttpResponse implements RemoteAuthResponse {
	private final HttpServletResponse response;

	public JavaxServletHttpResponse(HttpServletResponse response) {
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
