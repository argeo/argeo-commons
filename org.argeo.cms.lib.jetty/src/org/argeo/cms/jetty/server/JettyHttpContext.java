package org.argeo.cms.jetty.server;

import java.net.http.WebSocket;
import java.util.HashMap;
import java.util.Map;

import org.argeo.cms.jetty.AbstractJettyHttpContext;
import org.argeo.cms.jetty.ContextHandlerAttributes;
import org.argeo.cms.jetty.JettyHttpServer;
import org.argeo.cms.jetty.websocket.JettyLocalWebSocket;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.session.SessionHandler;
import org.eclipse.jetty.util.Callback;
import org.eclipse.jetty.websocket.server.ServerUpgradeRequest;
import org.eclipse.jetty.websocket.server.ServerUpgradeResponse;
import org.eclipse.jetty.websocket.server.WebSocketCreator;
import org.eclipse.jetty.websocket.server.WebSocketUpgradeHandler;

import com.sun.net.httpserver.HttpContext;

/**
 * A {@link HttpContext} based on pure Jetty server components (no dependency to
 * the jakarta/javax servlet APIs).
 */
public class JettyHttpContext extends AbstractJettyHttpContext {
	private final Handler httpHandler;
	private Map<String, Object> attributes;

	public JettyHttpContext(JettyHttpServer httpServer, String path) {
		super(httpServer, path);
		boolean useContextHandler = true;
		if (useContextHandler) {
			ContextHandler contextHandler = new ContextHandler();
			SessionHandler sessionHandler = new SessionHandler();
			contextHandler.setHandler(sessionHandler);
			sessionHandler.setHandler(new HttpContextJettyHandler(this));
			attributes = new ContextHandlerAttributes(contextHandler);
			httpHandler = contextHandler;
		} else {
			httpHandler = new HttpContextJettyHandler(this);
			attributes = new HashMap<>();
		}
	}

	@Override
	protected Handler getJettyHandler() {
		WebSocketUpgradeHandler webSocketUpgradeHandler = WebSocketUpgradeHandler.from(getJettyHttpServer().getServer(),
				(container) -> {
					container.addMapping(getPath(), new WebSocketCreator() {

						@Override
						public Object createWebSocket(ServerUpgradeRequest upgradeRequest,
								ServerUpgradeResponse upgradeResponse, Callback callback) throws Exception {
							if (getHandler() instanceof WebSocket.Listener webSocketListener) {
								return new JettyLocalWebSocket(webSocketListener);
							} else {
								callback.succeeded();
								return null;
							}
						}
					});
				});
		webSocketUpgradeHandler.setHandler(httpHandler);
		return webSocketUpgradeHandler;
	}

	@Override
	public Map<String, Object> getAttributes() {
		return attributes;
	}

}
