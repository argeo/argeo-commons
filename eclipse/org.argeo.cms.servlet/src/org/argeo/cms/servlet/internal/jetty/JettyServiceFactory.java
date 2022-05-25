package org.argeo.cms.servlet.internal.jetty;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;

import javax.websocket.DeploymentException;
import javax.websocket.server.ServerContainer;
import javax.websocket.server.ServerEndpointConfig;

import org.argeo.api.cms.CmsConstants;
import org.argeo.api.cms.CmsLog;
import org.argeo.cms.websocket.javax.server.CmsWebSocketConfigurator;
import org.argeo.cms.websocket.javax.server.TestEndpoint;
import org.argeo.util.LangUtils;
import org.eclipse.equinox.http.jetty.JettyConfigurator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedServiceFactory;
import org.osgi.util.tracker.ServiceTracker;

public class JettyServiceFactory implements ManagedServiceFactory {
	private final static CmsLog log = CmsLog.getLog(JettyServiceFactory.class);

	final static String CMS_JETTY_CUSTOMIZER_CLASS = "org.argeo.equinox.jetty.CmsJettyCustomizer";
	// Argeo specific
	final static String WEBSOCKET_ENABLED = "websocket.enabled";

	private final BundleContext bc = FrameworkUtil.getBundle(JettyServiceFactory.class).getBundleContext();

	public void start() {
		ServiceTracker<ServerContainer, ServerContainer> serverSt = new ServiceTracker<ServerContainer, ServerContainer>(
				bc, ServerContainer.class, null) {

			@Override
			public ServerContainer addingService(ServiceReference<ServerContainer> reference) {
				ServerContainer serverContainer = super.addingService(reference);

				BundleContext bc = reference.getBundle().getBundleContext();
				ServiceReference<ServerEndpointConfig.Configurator> srConfigurator = bc
						.getServiceReference(ServerEndpointConfig.Configurator.class);
				ServerEndpointConfig.Configurator endpointConfigurator = bc.getService(srConfigurator);
				ServerEndpointConfig config = ServerEndpointConfig.Builder
						.create(TestEndpoint.class, "/ws/test/events/").configurator(endpointConfigurator).build();
				try {
					serverContainer.addEndpoint(config);
				} catch (DeploymentException e) {
					throw new IllegalStateException("Cannot initalise the WebSocket server runtime.", e);
				}
				return serverContainer;
			}

		};
		serverSt.open();
	}

	@Override
	public String getName() {
		return "Jetty Service Factory";
	}

	@Override
	public void updated(String pid, Dictionary<String, ?> properties) throws ConfigurationException {
		// Explicitly configures Jetty so that the default server is not started by the
		// activator of the Equinox Jetty bundle.
		Map<String, String> config = LangUtils.dictToStringMap(properties);
		if (!config.isEmpty()) {
			config.put("customizer.class", CMS_JETTY_CUSTOMIZER_CLASS);

			// TODO centralise with Jetty extender
			Object webSocketEnabled = config.get(WEBSOCKET_ENABLED);
			if (webSocketEnabled != null && webSocketEnabled.toString().equals("true")) {
				bc.registerService(ServerEndpointConfig.Configurator.class, new CmsWebSocketConfigurator(), null);
				config.put(WEBSOCKET_ENABLED, "true");
			}
		}

		int tryCount = 60;
		try {
			tryGettyJetty: while (tryCount > 0) {
				try {
					// FIXME deal with multiple ids
					JettyConfigurator.startServer(CmsConstants.DEFAULT, new Hashtable<>(config));
					// Explicitly starts Jetty OSGi HTTP bundle, so that it gets triggered if OSGi
					// configuration is not cleaned
					FrameworkUtil.getBundle(JettyConfigurator.class).start();
					break tryGettyJetty;
				} catch (IllegalStateException e) {
					// Jetty may not be ready
					try {
						Thread.sleep(1000);
					} catch (Exception e1) {
						// silent
					}
					tryCount--;
				}
			}
		} catch (Exception e) {
			log.error("Cannot start default Jetty server with config " + properties, e);
		}

	}

	@Override
	public void deleted(String pid) {
	}

	public void stop() {
		try {
			JettyConfigurator.stopServer(CmsConstants.DEFAULT);
		} catch (Exception e) {
			log.error("Cannot stop default Jetty server.", e);
		}

	}

}
