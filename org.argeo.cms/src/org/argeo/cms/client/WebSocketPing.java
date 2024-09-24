package org.argeo.cms.client;

import java.io.IOException;
import java.math.RoundingMode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Version;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.text.DecimalFormat;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;

/** Tests connectivity to the web socket server. */
public class WebSocketPing implements Runnable {
	private final static int PING_FRAME_SIZE = 125;
	private final static DecimalFormat decimalFormat = new DecimalFormat("0.0");
	static {
		decimalFormat.setRoundingMode(RoundingMode.HALF_UP);
	}

	private final URI uri;
	private final UUID uuid;

	private WebSocket webSocket;

	public WebSocketPing(URI uri) {
		this.uri = uri;
		this.uuid = UUID.randomUUID();
	}

	@Override
	public void run() {
		try {
			WebSocket.Listener listener = new WebSocket.Listener() {

				@Override
				public CompletionStage<?> onPong(WebSocket webSocket, ByteBuffer message) {
					long msb = message.getLong();
					long lsb = message.getLong();
					long end = System.nanoTime();
					if (msb != uuid.getMostSignificantBits() || lsb != uuid.getLeastSignificantBits())
						return null; // ignore
					long begin = message.getLong();
					double durationNs = end - begin;
					double durationMs = durationNs / 1000000;
					int size = message.remaining() + (3 * Long.BYTES);
					System.out.println(
							size + " bytes from " + uri + ": time=" + decimalFormat.format(durationMs) + " ms");
					return null;
				}

			};

			//HttpClient client = HttpClient.newHttpClient();
			HttpClient client = HttpClient.newBuilder().version(Version.HTTP_2).build();
			try {
				HttpRequest request = HttpRequest.newBuilder().uri(URI.create("http://localhost:7070/status/ping/")).build();

				HttpResponse<String> response = client.send(request, BodyHandlers.ofString());

				System.out.println(response.body());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			CompletableFuture<WebSocket> ws = client.newWebSocketBuilder().buildAsync(uri, listener);
			webSocket = ws.get();
			webSocket.request(Long.MAX_VALUE);

			Runtime.getRuntime().addShutdownHook(new Thread(() -> webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "")));

			while (!webSocket.isInputClosed()) {
				long begin = System.nanoTime();
				ByteBuffer buffer = ByteBuffer.allocate(PING_FRAME_SIZE);
				buffer.putLong(uuid.getMostSignificantBits());
				buffer.putLong(uuid.getLeastSignificantBits());
				buffer.putLong(begin);
				buffer.flip();
				webSocket.sendPing(buffer);
				Thread.sleep(1000);
			}
		} catch (InterruptedException e) {
			if (webSocket != null)
				webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "");
		} catch (ExecutionException e) {
			throw new RuntimeException("Cannot ping " + uri, e.getCause());
		}
	}

	public static void main(String[] args) throws Exception {
		if (args.length == 0) {
			System.err.println("usage: java " + WebSocketPing.class.getName() + " <url>");
			System.exit(1);
			return;
		}
		URI uri = URI.create(args[0]);
		new WebSocketPing(uri).run();
	}

}
