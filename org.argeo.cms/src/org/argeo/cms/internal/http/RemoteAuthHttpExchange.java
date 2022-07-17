package org.argeo.cms.internal.http;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

import org.argeo.cms.auth.RemoteAuthRequest;
import org.argeo.cms.auth.RemoteAuthResponse;
import org.argeo.cms.auth.RemoteAuthSession;

import com.sun.net.httpserver.HttpExchange;

public class RemoteAuthHttpExchange implements RemoteAuthRequest, RemoteAuthResponse {
	private HttpExchange httpExchange;

	public RemoteAuthHttpExchange(HttpExchange httpExchange) {
		this.httpExchange = httpExchange;
	}

	@Override
	public void setHeader(String keys, String value) {
		httpExchange.getResponseHeaders().put(keys, Collections.singletonList(value));
	}

	@Override
	public RemoteAuthSession getSession() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public RemoteAuthSession createSession() {
		// TODO Auto-generated method stub
		return null;
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
