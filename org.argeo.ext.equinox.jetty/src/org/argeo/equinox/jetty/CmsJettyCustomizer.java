package org.argeo.equinox.jetty;

import java.util.Dictionary;

import javax.servlet.ServletException;

import org.eclipse.equinox.http.jetty.JettyCustomizer;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.websocket.jsr356.server.ServerContainer;
import org.eclipse.jetty.websocket.jsr356.server.deploy.WebSocketServerContainerInitializer;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;

/** Customises the Jetty HTTP server. */
public class CmsJettyCustomizer extends JettyCustomizer {
	private BundleContext bc = FrameworkUtil.getBundle(CmsJettyCustomizer.class).getBundleContext();

	@Override
	public Object customizeContext(Object context, Dictionary<String, ?> settings) {
		// WebSocket
		Object webSocketEnabled = settings.get("websocket.enabled");
		if (webSocketEnabled != null && webSocketEnabled.toString().equals("true")) {
			ServletContextHandler servletContextHandler = (ServletContextHandler) context;
			new WebSocketInit(servletContextHandler).start();
		}
		return super.customizeContext(context, settings);

	}

	/** Configure websocket container asynchronously as it may take some time */
	private class WebSocketInit extends Thread {
		ServletContextHandler servletContextHandler;

		public WebSocketInit(ServletContextHandler servletContextHandler) {
			super("WebSocket Init");
			this.servletContextHandler = servletContextHandler;
		}

		@Override
		public void run() {
			ServerContainer serverContainer;
			try {
				serverContainer = WebSocketServerContainerInitializer.configureContext(servletContextHandler);
			} catch (ServletException e) {
				throw new IllegalStateException("Cannot configure web sockets", e);
			}
			bc.registerService(javax.websocket.server.ServerContainer.class, serverContainer, null);
		}

	}
}
