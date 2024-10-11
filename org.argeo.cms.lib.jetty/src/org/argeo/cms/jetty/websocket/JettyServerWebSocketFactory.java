package org.argeo.cms.jetty.websocket;

import static org.argeo.api.cms.CmsConstants.CONTEXT_PATH;

import java.net.http.WebSocket;
import java.util.Map;

import org.argeo.api.cms.CmsConstants;
import org.argeo.api.cms.CmsLog;
import org.argeo.cms.jetty.JettyServer;
import org.eclipse.jetty.websocket.server.ServerWebSocketContainer;

/** Adds WebSocket mapping to an existing Jetty server. */
public class JettyServerWebSocketFactory {
	private final static CmsLog log = CmsLog.getLog(JettyServerWebSocketFactory.class);

	private ServerWebSocketContainer container;

	public void setJettyServer(JettyServer jettyServer) {
		container = jettyServer.getServerWebSocketContainer();
		log.debug("WebSocket support initalized");
	}

	public void addWebSocket(WebSocket.Listener webSocket, Map<String, String> properties) {
		String path = properties.get(CmsConstants.CONTEXT_PATH);
		if (path == null) {
			log.warn("Property " + CONTEXT_PATH + " not set on HTTP handler " + properties + ". Ignoring it.");
			return;
		}

		container.addMapping(path, (upgradeRequest, upgradeResponse, callback) -> {
			log.debug("Adding " + path + " WebSocket " + webSocket.getClass());
			return new JettyLocalWebSocket(webSocket);
		});
	}

	public void removeWebSocket(WebSocket.Listener webSocket, Map<String, String> properties) {
		String path = properties.get(CmsConstants.CONTEXT_PATH);
		if (path == null) {
			log.warn("Property " + CONTEXT_PATH + " not set on HTTP handler " + properties + ". Ignoring it.");
			return;
		}

		container.addMapping(path, (upgradeRequest, upgradeResponse, callback) -> {
			// disable web socket for this path
			log.debug("Removing " + path + " WebSocket " + webSocket.getClass());
			// TODO check that it works and that mappings can be removed dynamically
			callback.succeeded();
			return null;
		});
	}
}
