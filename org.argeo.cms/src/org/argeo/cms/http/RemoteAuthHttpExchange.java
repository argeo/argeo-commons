package org.argeo.cms.http;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import org.argeo.cms.auth.RemoteAuthRequest;
import org.argeo.cms.auth.RemoteAuthResponse;
import org.argeo.cms.auth.RemoteAuthSession;

import com.sun.net.httpserver.HttpExchange;

/**
 * Implementation of {@link RemoteAuthRequest} and {@link RemoteAuthResponse}
 * based on {@link HttpExchange}.
 */
public class RemoteAuthHttpExchange implements RemoteAuthRequest, RemoteAuthResponse {
	private final HttpExchange httpExchange;
	private RemoteAuthSession remoteAuthSession;

	public RemoteAuthHttpExchange(HttpExchange httpExchange) {
		this.httpExchange = httpExchange;
		this.remoteAuthSession = (RemoteAuthSession) httpExchange.getAttribute(RemoteAuthSession.class.getName());
		Objects.requireNonNull(this.remoteAuthSession);
	}

	@Override
	public void setHeader(String headerName, String value) {
		httpExchange.getResponseHeaders().put(headerName, Collections.singletonList(value));
	}

	@Override
	public void addHeader(String headerName, String value) {
		List<String> values = httpExchange.getResponseHeaders().getOrDefault(headerName, new ArrayList<>());
		values.add(value);
	}

	@Override
	public RemoteAuthSession getSession() {
		return remoteAuthSession;
	}

	@Override
	public RemoteAuthSession createSession() {
		throw new UnsupportedOperationException("Cannot create remote session");
	}

	@Override
	public Locale getLocale() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object getAttribute(String key) {
		return httpExchange.getAttribute(key);
	}

	@Override
	public void setAttribute(String key, Object object) {
		httpExchange.setAttribute(key, object);
	}

	@Override
	public String getHeader(String key) {
		List<String> lst = httpExchange.getRequestHeaders().get(key);
		if (lst == null || lst.size() == 0)
			return null;
		return lst.get(0);
	}

	@Override
	public int getLocalPort() {
		return httpExchange.getLocalAddress().getPort();
	}

	@Override
	public String getRemoteAddr() {
		return httpExchange.getRemoteAddress().getHostName();
	}

	@Override
	public int getRemotePort() {
		return httpExchange.getRemoteAddress().getPort();
	}

}
