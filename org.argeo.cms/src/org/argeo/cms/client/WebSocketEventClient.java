package org.argeo.cms.client;

import java.net.URI;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;

/** Tests connectivity to the web socket server. */
public class WebSocketEventClient implements Runnable {

	private final URI uri;

	private WebSocket webSocket;

	private CmsClient cmsClient;

	public WebSocketEventClient(URI uri) {
		this.uri = uri;
		cmsClient = new CmsClient(uri);
	}

	@Override
	public void run() {
		try {
			CompletableFuture<WebSocket> ws = cmsClient.newWebSocket(new WsEventListener());

			WebSocket webSocket = ws.get();
			webSocket.request(Long.MAX_VALUE);

			Runtime.getRuntime().addShutdownHook(new Thread(() -> webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "")));

			while (!webSocket.isInputClosed()) {
				webSocket.sendPing(ByteBuffer.allocate(0));
				Thread.sleep(10000);
			}
		} catch (InterruptedException e) {
			if (webSocket != null)
				webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "");
		} catch (ExecutionException e) {
			throw new RuntimeException("Cannot listent to " + uri, e.getCause());
		}
	}

	private class WsEventListener implements WebSocket.Listener {
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

	}
}
