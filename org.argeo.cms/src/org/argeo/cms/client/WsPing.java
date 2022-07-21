package org.argeo.cms.client;

import java.math.RoundingMode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.text.DecimalFormat;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;

/** Tests connectivity to the web socket server. */
public class WsPing implements Runnable {
	private final static int PING_FRAME_SIZE = 125;

	private final URI uri;
	private final UUID uuid;

	private final DecimalFormat decimalFormat;

	public WsPing(URI uri) {
		this.uri = uri;
		this.uuid = UUID.randomUUID();
		decimalFormat = new DecimalFormat("0.0");
		decimalFormat.setRoundingMode(RoundingMode.HALF_UP);
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

			HttpClient client = HttpClient.newHttpClient();
			CompletableFuture<WebSocket> ws = client.newWebSocketBuilder().buildAsync(uri, listener);
			WebSocket webSocket = ws.get();
			webSocket.request(Long.MAX_VALUE);

			Runtime.getRuntime().addShutdownHook(new Thread(() -> webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "")));

			while (!webSocket.isInputClosed()) {
				long begin = System.nanoTime();
//				ByteBuffer buffer = ByteBuffer.allocate(3 * Long.BYTES);
				ByteBuffer buffer = ByteBuffer.allocate(PING_FRAME_SIZE);
				buffer.putLong(uuid.getMostSignificantBits());
				buffer.putLong(uuid.getLeastSignificantBits());
				buffer.putLong(begin);
				buffer.flip();
				webSocket.sendPing(buffer);
				Thread.sleep(1000);
			}
		} catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) throws Exception {
		if (args.length == 0) {
			System.err.println("usage: java " + WsPing.class.getName() + " <url>");
			System.exit(1);
			return;
		}
		URI uri = URI.create(args[0]);
		new WsPing(uri).run();
	}

}
