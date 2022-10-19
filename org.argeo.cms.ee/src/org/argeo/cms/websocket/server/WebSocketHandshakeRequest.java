package org.argeo.cms.websocket.server;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import javax.servlet.http.HttpSession;
import javax.websocket.server.HandshakeRequest;

import org.argeo.cms.auth.RemoteAuthRequest;
import org.argeo.cms.auth.RemoteAuthSession;
import org.argeo.cms.servlet.ServletHttpSession;

public class WebSocketHandshakeRequest implements RemoteAuthRequest {
	private final HandshakeRequest handshakeRequest;
	private final HttpSession httpSession;

	private Map<String, Object> attributes = new HashMap<>();

	public WebSocketHandshakeRequest(HandshakeRequest handshakeRequest) {
		Objects.requireNonNull(handshakeRequest);
		this.handshakeRequest = handshakeRequest;
		this.httpSession = (HttpSession) handshakeRequest.getHttpSession();
//		Objects.requireNonNull(this.httpSession);
	}

	@Override
	public RemoteAuthSession getSession() {
		if (httpSession == null)
			return null;
		return new ServletHttpSession(httpSession);
	}

	@Override
	public RemoteAuthSession createSession() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Locale getLocale() {
		// TODO check Accept-Language header
		return Locale.getDefault();
	}

	@Override
	public Object getAttribute(String key) {
		return attributes.get(key);
	}

	@Override
	public void setAttribute(String key, Object object) {
		attributes.put(key, object);
	}

	@Override
	public String getHeader(String key) {
		List<String> values = handshakeRequest.getHeaders().get(key);
		if (values.size() == 0)
			return null;
		if (values.size() > 1)
			throw new IllegalStateException("More that one value for " + key + ": " + values);
		return values.get(0);
	}

	@Override
	public String getRemoteAddr() {
		throw new UnsupportedOperationException();
	}

	@Override
	public int getLocalPort() {
		throw new UnsupportedOperationException();
	}

	@Override
	public int getRemotePort() {
		throw new UnsupportedOperationException();
	}

}
