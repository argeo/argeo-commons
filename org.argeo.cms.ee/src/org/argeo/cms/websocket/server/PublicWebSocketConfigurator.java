package org.argeo.cms.websocket.server;

import org.argeo.cms.auth.RemoteAuthRequest;
import org.argeo.cms.auth.RemoteAuthResponse;

public class PublicWebSocketConfigurator extends CmsWebSocketConfigurator {

	@Override
	protected boolean authIsRequired(RemoteAuthRequest remoteAuthRequest, RemoteAuthResponse remoteAuthResponse) {
		return false;
	}

}
