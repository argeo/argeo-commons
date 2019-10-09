package org.argeo.equinox.jetty;

import java.util.Dictionary;

import javax.servlet.ServletContext;
import javax.websocket.DeploymentException;

import org.eclipse.equinox.http.jetty.JettyCustomizer;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.websocket.jsr356.server.ServerContainer;
import org.eclipse.jetty.websocket.jsr356.server.deploy.WebSocketServerContainerInitializer;
import org.eclipse.jetty.websocket.jsr356.server.deploy.WebSocketServerContainerInitializer.Configurator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;

/** Customises the Jetty HTTP server. */
public class CmsJettyCustomizer extends JettyCustomizer {
	private BundleContext bc = FrameworkUtil.getBundle(CmsJettyCustomizer.class).getBundleContext();

	@Override
	public Object customizeContext(Object context, Dictionary<String, ?> settings) {
		// WebSocket
		Object webSocketEnabled = settings.get("websocket.enabled");
		if (webSocketEnabled == null) {
			webSocketEnabled = bc.getProperty("org.eclipse.equinox.http.jetty.websocket.enabled");
		}
		if (webSocketEnabled != null && webSocketEnabled.toString().equals("true")) {
			ServletContextHandler servletContextHandler = (ServletContextHandler) context;
			WebSocketServerContainerInitializer.configure(servletContextHandler, new Configurator() {

				@Override
				public void accept(ServletContext servletContext, ServerContainer serverContainer)
						throws DeploymentException {
					bc.registerService(javax.websocket.server.ServerContainer.class, serverContainer, null);
				}
			});
		}
		return super.customizeContext(context, settings);

	}
}
