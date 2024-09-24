package org.argeo.cms.jetty.websocket;

import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;

import org.eclipse.jetty.websocket.api.Callback;
import org.eclipse.jetty.websocket.api.Session;

/**
 * A {@link java.net.http.WebSocket} wrapping a Jetty WebSocket API
 * {@link Session}. This is the "client" interface of a server-side socket,
 * which allows to interact with the remote endpoint.
 */
class JettyJavaWebSocket implements WebSocket {
	private Session session;

	JettyJavaWebSocket(Session session) {
		this.session = session;
	}

	@Override
	public CompletableFuture<WebSocket> sendText(CharSequence data, boolean last) {
		return Callback.Completable.with(completable -> session.sendText(data.toString(), completable))
				.thenApply((v) -> JettyJavaWebSocket.this);
	}

	@Override
	public CompletableFuture<WebSocket> sendBinary(ByteBuffer data, boolean last) {
		return Callback.Completable.with(completable -> session.sendBinary(data, completable))
				.thenApply((v) -> JettyJavaWebSocket.this);
	}

	@Override
	public CompletableFuture<WebSocket> sendPing(ByteBuffer message) {
		return Callback.Completable.with(completable -> session.sendPing(message, completable))
				.thenApply((v) -> JettyJavaWebSocket.this);
	}

	@Override
	public CompletableFuture<WebSocket> sendPong(ByteBuffer message) {
		return Callback.Completable.with(completable -> session.sendPong(message, completable))
				.thenApply((v) -> JettyJavaWebSocket.this);
	}

	@Override
	public CompletableFuture<WebSocket> sendClose(int statusCode, String reason) {
		return Callback.Completable.with(completable -> session.close(statusCode, reason, completable))
				.thenApply((v) -> JettyJavaWebSocket.this);
	}

	@Override
	public void request(long n) {
		for (long i = 0; i < n; i++) {
			// TODO throttle it somehow?
			session.demand();
		}
	}

	@Override
	public String getSubprotocol() {
		// TODO test this
		return session.getUpgradeResponse().getAcceptedSubProtocol();
	}

	@Override
	public boolean isOutputClosed() {
		// TODO make sure the semantics are similar
		return !session.isOpen();
	}

	@Override
	public boolean isInputClosed() {
		// TODO make sure the semantics are similar
		return !session.isOpen();
	}

	@Override
	public void abort() {
		session.disconnect();
	}

}
