package org.argeo.cms.servlet;

import java.util.Objects;

import javax.servlet.http.HttpServletResponse;

import org.argeo.cms.auth.HttpResponse;

public class ServletHttpResponse implements HttpResponse {
	private final HttpServletResponse response;

	public ServletHttpResponse(HttpServletResponse response) {
		Objects.requireNonNull(response);
		this.response = response;
	}

	@Override
	public void setHeader(String keys, String value) {
		response.setHeader(keys, value);
	}

}
