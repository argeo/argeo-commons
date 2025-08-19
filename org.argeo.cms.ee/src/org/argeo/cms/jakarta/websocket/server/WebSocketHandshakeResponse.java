package org.argeo.cms.jakarta.websocket.server;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import jakarta.websocket.HandshakeResponse;

import org.argeo.cms.auth.RemoteAuthResponse;

public class WebSocketHandshakeResponse implements RemoteAuthResponse {
	private final HandshakeResponse handshakeResponse;

	public WebSocketHandshakeResponse(HandshakeResponse handshakeResponse) {
		this.handshakeResponse = handshakeResponse;
	}

	@Override
	public void setHeader(String headerName, String value) {
		handshakeResponse.getHeaders().put(headerName, Collections.singletonList(value));
	}

	@Override
	public void addHeader(String headerName, String value) {
		List<String> values = handshakeResponse.getHeaders().getOrDefault(headerName, new ArrayList<>());
		values.add(value);
	}

}
