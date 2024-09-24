package org.argeo.cms.internal.http;

import java.io.IOException;
import java.net.http.WebSocket;
import java.net.http.WebSocket.Listener;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletionStage;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

/** A trivial ping WebSocket. */
public class PingWebSocket implements Listener, HttpHandler {

	@Override
	public CompletionStage<?> onPing(WebSocket webSocket, ByteBuffer message) {
		return null;
	}

	@Override
	public void handle(HttpExchange exchange) throws IOException {
		String message = "pong";
		byte[] arr = message.getBytes();
		exchange.sendResponseHeaders(200, arr.length);
		try {
			exchange.getResponseBody().write(arr);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw e;
		}
		exchange.getResponseBody().close();
	}

}
