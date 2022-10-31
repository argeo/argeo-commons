package org.argeo.cms.client;

import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;

import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import org.argeo.cms.auth.RemoteAuthUtils;
import org.argeo.util.http.HttpHeader;

/** Tests connectivity to the web socket server. */
public class WebSocketEventClient implements Runnable {

	private final URI uri;

	private WebSocket webSocket;
	
	public WebSocketEventClient(URI uri) {
		this.uri = uri;
	}

	@Override
	public void run() {
		try {
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

			// SPNEGO
			URL jaasUrl = SpnegoHttpClient.class.getResource("jaas-client-ipa.cfg");
			System.setProperty("java.security.auth.login.config", jaasUrl.toExternalForm());
			LoginContext lc = new LoginContext(SpnegoHttpClient.CLIENT_LOGIN_CONTEXT);
			lc.login();
			String token = RemoteAuthUtils.createGssToken(lc.getSubject(), "HTTP", uri.getHost());

			HttpClient client = SpnegoHttpClient.openHttpClient(lc.getSubject());
			CompletableFuture<WebSocket> ws = client.newWebSocketBuilder()
					.header(HttpHeader.AUTHORIZATION.getHeaderName(), HttpHeader.NEGOTIATE + " " + token)
					.buildAsync(uri, listener);

			WebSocket webSocket = ws.get();
			webSocket.request(Long.MAX_VALUE);

			Runtime.getRuntime().addShutdownHook(new Thread(() -> webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "")));

			while (!webSocket.isInputClosed()) {
				webSocket.sendPing(ByteBuffer.allocate(0));
				Thread.sleep(10000);
			}
		}catch (InterruptedException e) {
			if (webSocket != null)
				webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "");
		} catch (ExecutionException | LoginException e) {
			throw new RuntimeException("Cannot listent to " + uri, e.getCause());
		}
	}

//	public static void main(String[] args) throws Exception {
//		if (args.length == 0) {
//			System.err.println("usage: java " + WebSocketEventClient.class.getName() + " <url>");
//			System.exit(1);
//			return;
//		}
//		URI uri = URI.create(args[0]);
//	}

}
