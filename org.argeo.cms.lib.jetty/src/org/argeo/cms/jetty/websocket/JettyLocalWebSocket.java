package org.argeo.cms.jetty.websocket;

import java.net.http.WebSocket;
import java.net.http.WebSocket.Listener;
import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.concurrent.CompletionStage;

import org.eclipse.jetty.websocket.api.Callback;
import org.eclipse.jetty.websocket.api.Frame;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketError;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketFrame;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketOpen;

/**
 * Wrap a {@link java.net.http.WebSocket.Listener} with Jetty WebSocket API
 * annotations. This is the actual "server"/local side of a WebSocket.
 */
@org.eclipse.jetty.websocket.api.annotations.WebSocket(autoDemand = false)
public class JettyLocalWebSocket {
	private WebSocket.Listener listener;

	public JettyLocalWebSocket(Listener listener) {
		this.listener = listener;
	}

	@OnWebSocketOpen
	public void onOpen(Session session) {
		//session.demand();
		listener.onOpen(wrap(session));
	}

	@OnWebSocketMessage
	public void onText(Session session, String text, boolean last) {
		waitFor(listener.onText(wrap(session), text, last));
	}

	@OnWebSocketMessage
	public void onBinary(Session session, ByteBuffer data, boolean last, Callback callback) {
		notifyCallback(listener.onBinary(wrap(session), data, last), callback);
	}

	@OnWebSocketFrame
	public void onFrame(Session session, Frame frame, Callback callback) {
		if (Frame.Type.PING.equals(frame.getType())) {
			notifyCallback(listener.onPing(wrap(session), frame.getPayload()), callback);
		} else if (Frame.Type.PONG.equals(frame.getType())) {
			notifyCallback(listener.onPong(wrap(session), frame.getPayload()), callback);
		}
	}

	@OnWebSocketClose
	public void onClose(Session session, int statusCode, String reason) {
		waitFor(listener.onClose(wrap(session), statusCode, reason));
	}

	@OnWebSocketError
	public void onError(Session session, Throwable error) {
		listener.onError(wrap(session), error);
	}

	/*
	 * UTILITIES
	 */
	protected WebSocket wrap(Session session) {
		return new JettyJavaWebSocket(session);
	}

	protected void waitFor(CompletionStage<?> stage) {
		if (stage == null)
			return;
		stage.toCompletableFuture().join();
	}

	protected void notifyCallback(CompletionStage<?> stage, Callback callback) {
		Objects.requireNonNull(callback);
		if (stage == null) {
			callback.succeed();
			return;
		}
		stage.exceptionally((t) -> {// failure
			callback.fail(t);
			return null;
		}).thenRun(() -> {// success
			callback.succeed();
		});
	}
}
