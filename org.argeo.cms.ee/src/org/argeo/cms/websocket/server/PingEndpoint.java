package org.argeo.cms.websocket.server;

import java.nio.channels.ClosedChannelException;

import javax.websocket.OnError;
import javax.websocket.server.ServerEndpoint;

import org.argeo.api.cms.CmsLog;

@ServerEndpoint(value = "/ping", configurator = PublicWebSocketConfigurator.class)
public class PingEndpoint {
	private final static CmsLog log = CmsLog.getLog(PingEndpoint.class);

	@OnError
	public void onError(Throwable e) {
		if (e instanceof ClosedChannelException) {
			// ignore, as it probably means ping was closed on the other side
			return;
		}
		log.error("Cannot process ping", e);
	}
}
