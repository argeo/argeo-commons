package org.argeo.cms.websocket.server;

import java.io.IOException;
import java.util.Map;

import javax.websocket.OnClose;
import javax.websocket.OnOpen;
import javax.websocket.RemoteEndpoint;
import javax.websocket.Session;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;

import org.argeo.api.cms.CmsEventBus;
import org.argeo.api.cms.CmsEventSubscriber;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;

@ServerEndpoint(value = "/event/{topic}", configurator = CmsWebSocketConfigurator.class)
public class EventEndpoint implements CmsEventSubscriber {
	private BundleContext bc = FrameworkUtil.getBundle(TestEndpoint.class).getBundleContext();

	private RemoteEndpoint.Basic remote;
	private CmsEventBus cmsEventBus;

//	private String topic = "cms";

	@OnOpen
	public void onOpen(Session session, @PathParam("topic") String topic) {
		if (bc != null) {
			cmsEventBus = bc.getService(bc.getServiceReference(CmsEventBus.class));
			cmsEventBus.addEventSubscriber(topic, this);
		}
		remote = session.getBasicRemote();

	}

	@OnClose
	public void onClose(@PathParam("topic") String topic) {
		cmsEventBus.removeEventSubscriber(topic, this);
	}

	@Override
	public void onEvent(String topic, Map<String, Object> properties) {
		try {
			remote.sendText(topic + ": " + properties);
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}
}
