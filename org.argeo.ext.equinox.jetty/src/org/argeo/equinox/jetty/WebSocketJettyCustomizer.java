package org.argeo.equinox.jetty;

import java.util.Dictionary;

import javax.servlet.ServletException;
import javax.websocket.server.ServerContainer;

import org.eclipse.equinox.http.jetty.JettyCustomizer;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.websocket.jsr356.server.deploy.WebSocketServerContainerInitializer;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;

public class WebSocketJettyCustomizer extends JettyCustomizer {
	private BundleContext bc = FrameworkUtil.getBundle(WebSocketJettyCustomizer.class).getBundleContext();

	@Override
	public Object customizeContext(Object context, Dictionary<String, ?> settings) {
		ServletContextHandler servletContextHandler = (ServletContextHandler) context;
		new WebSocketInit(servletContextHandler).start();
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
			bc.registerService(ServerContainer.class, serverContainer, null);
//			ServiceTracker<ServerEndpointConfig.Builder, ServerEndpointConfig.Builder> endpointsTracker = new ServiceTracker<ServerEndpointConfig.Builder, ServerEndpointConfig.Builder>(
//					bc, ServerEndpointConfig.Builder.class, null) {
//
//				@Override
//				public ServerEndpointConfig.Builder addingService(
//						ServiceReference<ServerEndpointConfig.Builder> reference) {
//
//					ServerEndpointConfig.Builder serverEndpointConfig = super.addingService(reference);
//					try {
//						serverContainer.addEndpoint(serverEndpointConfig.build());
//					} catch (DeploymentException e) {
//						throw new IllegalArgumentException("Cannot add end point " + reference, e);
//					}
//					return serverEndpointConfig;
//				}
//			};
//			endpointsTracker.open();
			// TODO log it properly
			// TODO close itproperly
		}

	}

}
