package org.argeo.cms.jakarta.websocket.server;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;

/** Tests connectivity to the web socket server. */
public class WebSocketTest {

	public static void main(String[] args) throws Exception {
		CompletableFuture<Boolean> received = new CompletableFuture<>();
		WebSocket.Listener listener = new WebSocket.Listener() {

			public CompletionStage<?> onText(WebSocket webSocket, CharSequence message, boolean last) {
				System.out.println(message);
				CompletionStage<String> res = CompletableFuture.completedStage(message.toString());
				received.complete(true);
				return res;
			}
		};

		HttpClient client = HttpClient.newHttpClient();
		CompletableFuture<WebSocket> ws = client.newWebSocketBuilder()
				.buildAsync(URI.create("ws://localhost:7070/cms/status/test/my%20topic?path=my%2Frelative%2Fpath"), listener);
		WebSocket webSocket = ws.get();
		webSocket.sendText("TEST", true);

		received.get(10, TimeUnit.SECONDS);
		webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "");
	}

}
