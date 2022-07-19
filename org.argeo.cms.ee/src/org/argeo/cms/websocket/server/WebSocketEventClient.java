package org.argeo.cms.websocket.server;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/** Tests connectivity to the web socket server. */
public class WebSocketEventClient {

	public static void main(String[] args) throws Exception {
		WebSocket.Listener listener = new WebSocket.Listener() {

			public CompletionStage<?> onText(WebSocket webSocket, CharSequence message, boolean last) {
				System.out.println(message);
				CompletionStage<String> res = CompletableFuture.completedStage(message.toString());
				return res;
			}

			@Override
			public CompletionStage<?> onPong(WebSocket webSocket, ByteBuffer message) {
				// System.out.println("Pong received.");
				return null;
			}

		};

		HttpClient client = HttpClient.newHttpClient();
		CompletableFuture<WebSocket> ws = client.newWebSocketBuilder()
				.buildAsync(URI.create("ws://localhost:7070/cms/status/event/cms"), listener);
		WebSocket webSocket = ws.get();
		webSocket.request(Long.MAX_VALUE);

		Runtime.getRuntime().addShutdownHook(new Thread(() -> webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "")));

		while (!webSocket.isInputClosed()) {
			webSocket.sendPing(ByteBuffer.allocate(0));
			Thread.sleep(10000);
		}
	}

}
