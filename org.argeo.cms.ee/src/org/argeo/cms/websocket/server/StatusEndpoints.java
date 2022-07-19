package org.argeo.cms.websocket.server;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class StatusEndpoints implements WebsocketEndpoints, HttpHandler {

	@Override
	public Set<Class<?>> getEndPoints() {
		Set<Class<?>> res = new HashSet<>();
		res.add(EventEndpoint.class);
		res.add(TestEndpoint.class);
		return res;
	}

	@Override
	public void handle(HttpExchange exchange) throws IOException {
		// web socket only
		exchange.sendResponseHeaders(200, -1);
	}

}
