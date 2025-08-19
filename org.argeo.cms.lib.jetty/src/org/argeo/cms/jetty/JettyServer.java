package org.argeo.cms.jetty;

import java.util.function.Supplier;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.websocket.server.ServerWebSocketContainer;

/** A web server whose underlying implementation is a Jetty server. */
public interface JettyServer extends Supplier<Server> {

	Integer getHttpPort();

	Integer getHttpsPort();

	String getHost();

	ServerWebSocketContainer getServerWebSocketContainer();

	default boolean isStarted() {
		return get().isStarted();
	}
}
