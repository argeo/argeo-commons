package org.argeo.api.cms.http;

import java.net.http.WebSocket;

public interface WebSocketHttpServer {

	WebSocket.Builder newWebSocketBuilder() ;
}
