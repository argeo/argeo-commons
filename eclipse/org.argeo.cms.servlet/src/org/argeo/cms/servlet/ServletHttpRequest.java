package org.argeo.cms.servlet;

import java.util.Locale;
import java.util.Objects;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.argeo.cms.auth.RemoteAuthRequest;
import org.argeo.cms.auth.RemoteAuthSession;

public class ServletHttpRequest implements RemoteAuthRequest {
	private final HttpServletRequest request;

	public ServletHttpRequest(HttpServletRequest request) {
		Objects.requireNonNull(request);
		this.request = request;
	}

	@Override
	public RemoteAuthSession getSession() {
		HttpSession httpSession = request.getSession(false);
		if (httpSession == null)
			return null;
		return new ServletHttpSession(httpSession);
	}

	@Override
	public RemoteAuthSession createSession() {
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
