package org.argeo.cms.jetty.server;

import java.net.http.WebSocket;
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
import com.sun.net.httpserver.HttpHandler;

/**
 * A {@link HttpContext} based on pure Jetty server API components (no
 * dependency to the jakarta/javax servlet APIs).
 */
public class JettyHttpContext extends AbstractJettyHttpContext {
	private final Handler jettyHandler;
	private Map<String, Object> attributes;

	public JettyHttpContext(JettyHttpServer httpServer, String path) {
		super(httpServer, path);
		ContextHandler contextHandler = new ContextHandler();
		SessionHandler sessionHandler = new SessionHandler();
		contextHandler.setHandler(sessionHandler);
		// TODO setting paths messes up with sessions
//		contextHandler.setContextPath(path);
//		sessionHandler.setSessionPath("/");
		HttpContextJettyHandler httpContextJettyHandler = new HttpContextJettyHandler(this);
		sessionHandler.setHandler(httpContextJettyHandler);
		attributes = new ContextHandlerAttributes(contextHandler);
		jettyHandler = contextHandler;
	}

	@Override
	protected Handler getJettyHandler() {
		// TODO optimize
		WebSocketUpgradeHandler webSocketUpgradeHandler = WebSocketUpgradeHandler.from(getJettyHttpServer().get(),
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
		webSocketUpgradeHandler.setHandler(jettyHandler);
		return webSocketUpgradeHandler;
//		return jettyHandler;
	}

	/*
	 * HttpContext CUSTOMISATIONS
	 */

	@Override
	public Map<String, Object> getAttributes() {
		return attributes;
	}

	@Override
	public void setHandler(HttpHandler handler) {
		super.setHandler(handler);
	}

}
