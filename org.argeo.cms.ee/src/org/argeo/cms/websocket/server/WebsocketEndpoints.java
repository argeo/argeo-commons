package org.argeo.cms.websocket.server;

import java.util.Set;

/** Configure web socket in Jetty without hard dependency. */
public interface WebsocketEndpoints {
	Set<Class<?>> getEndPoints();

}
