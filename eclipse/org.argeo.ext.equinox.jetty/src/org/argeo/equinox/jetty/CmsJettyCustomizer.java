package org.argeo.equinox.jetty;

import java.util.Dictionary;

import javax.servlet.ServletContext;
import javax.websocket.DeploymentException;
import javax.websocket.server.ServerContainer;

import org.eclipse.equinox.http.jetty.JettyCustomizer;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.websocket.javax.server.config.JavaxWebSocketServletContainerInitializer;
import org.eclipse.jetty.websocket.javax.server.config.JavaxWebSocketServletContainerInitializer.Configurator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;

/** Customises the Jetty HTTP server. */
public class CmsJettyCustomizer extends JettyCustomizer {
	private BundleContext bc = FrameworkUtil.getBundle(CmsJettyCustomizer.class).getBundleContext();

	public final static String WEBSOCKET_ENABLED = "websocket.enabled";

	@Override
	public Object customizeContext(Object context, Dictionary<String, ?> settings) {
		// WebSocket
		Object webSocketEnabled = settings.get(WEBSOCKET_ENABLED);
		if (webSocketEnabled != null && webSocketEnabled.toString().equals("true")) {
			ServletContextHandler servletContextHandler = (ServletContextHandler) context;
			JavaxWebSocketServletContainerInitializer.configure(servletContextHandler, new Configurator() {

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
