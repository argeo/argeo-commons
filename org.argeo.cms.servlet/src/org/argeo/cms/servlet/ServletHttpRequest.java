package org.argeo.cms.servlet;

import java.util.Locale;
import java.util.Objects;

import javax.servlet.http.HttpServletRequest;

import org.argeo.cms.auth.HttpRequest;
import org.argeo.cms.auth.HttpSession;

public class ServletHttpRequest implements HttpRequest {
	private final HttpServletRequest request;

	public ServletHttpRequest(HttpServletRequest request) {
		Objects.requireNonNull(request);
		this.request = request;
	}

	@Override
	public HttpSession getSession() {
		return new ServletHttpSession(request.getSession(false));
	}

	@Override
	public HttpSession createSession() {
		return new ServletHttpSession(request.getSession(true));
	}

	@Override
	public Locale getLocale() {
		return request.getLocale();
	}

	@Override
	public Object getAttribute(String key) {
		return request.getAttribute(key);
	}

	@Override
	public void setAttribute(String key, Object object) {
		request.setAttribute(key, object);
	}

	@Override
	public String getHeader(String key) {
		return request.getHeader(key);
	}

	@Override
	public String getRemoteAddr() {
		return request.getRemoteAddr();
	}

	@Override
	public int getLocalPort() {
		return request.getLocalPort();
	}

	@Override
	public int getRemotePort() {
		return request.getRemotePort();
	}
}
