package org.argeo.cms.websocket.server;

import java.util.Collections;

import javax.websocket.HandshakeResponse;

import org.argeo.cms.auth.RemoteAuthResponse;

public class WebSocketHandshakeResponse implements RemoteAuthResponse {
	private final HandshakeResponse handshakeResponse;

	public WebSocketHandshakeResponse(HandshakeResponse handshakeResponse) {
		this.handshakeResponse = handshakeResponse;
	}

	@Override
	public void setHeader(String key, String value) {
		handshakeResponse.getHeaders().put(key, Collections.singletonList(value));

	}

}
