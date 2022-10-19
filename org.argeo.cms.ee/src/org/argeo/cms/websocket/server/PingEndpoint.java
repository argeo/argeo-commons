package org.argeo.cms.websocket.server;

import javax.websocket.OnError;
import javax.websocket.server.ServerEndpoint;

import org.argeo.api.cms.CmsLog;

@ServerEndpoint(value = "/ping", configurator = PublicWebSocketConfigurator.class)
public class PingEndpoint {
	private final static CmsLog log = CmsLog.getLog(PingEndpoint.class);

	@OnError
	public void onError(Throwable e) {
		log.error("Cannot process ping", e);
	}
}
