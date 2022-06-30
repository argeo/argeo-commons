package org.argeo.cms.servlet.internal.jetty;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.ForkJoinPool;

import javax.websocket.DeploymentException;
import javax.websocket.server.ServerContainer;
import javax.websocket.server.ServerEndpointConfig;

import org.argeo.api.cms.CmsConstants;
import org.argeo.api.cms.CmsLog;
import org.argeo.api.cms.CmsState;
import org.argeo.cms.CmsDeployProperty;
import org.argeo.cms.websocket.javax.server.CmsWebSocketConfigurator;
import org.argeo.cms.websocket.javax.server.TestEndpoint;
import org.argeo.util.LangUtils;
import org.eclipse.equinox.http.jetty.JettyConfigurator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

public class JettyConfig {
	private final static CmsLog log = CmsLog.getLog(JettyConfig.class);

	final static String CMS_JETTY_CUSTOMIZER_CLASS = "org.argeo.equinox.jetty.CmsJettyCustomizer";

	private CmsState cmsState;

	private final BundleContext bc = FrameworkUtil.getBundle(JettyConfig.class).getBundleContext();

	//private static final String JETTY_PROPERTY_PREFIX = "org.eclipse.equinox.http.jetty.";

	public void start() {
		// We need to start asynchronously so that Jetty bundle get started by lazy init
		// due to the non-configurable behaviour of its activator
		ForkJoinPool.commonPool().execute(() -> {
			Dictionary<String, ?> properties = getHttpServerConfig();
			startServer(properties);
		});

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

		// check initialisation
//		ServiceTracker<?, ?> httpSt = new ServiceTracker<HttpService, HttpService>(bc, HttpService.class, null) {
//
//			@Override
//			public HttpService addingService(ServiceReference<HttpService> sr) {
//				Object httpPort = sr.getProperty("http.port");
//				Object httpsPort = sr.getProperty("https.port");
//				log.info(httpPortsMsg(httpPort, httpsPort));
//				close();
//				return super.addingService(sr);
//			}
//		};
//		httpSt.open();
	}

	public void stop() {
		try {
			JettyConfigurator.stopServer(CmsConstants.DEFAULT);
		} catch (Exception e) {
			log.error("Cannot stop default Jetty server.", e);
		}

	}

	public void startServer(Dictionary<String, ?> properties) {
		// Explicitly configures Jetty so that the default server is not started by the
		// activator of the Equinox Jetty bundle.
		Map<String, String> config = LangUtils.dictToStringMap(properties);
		if (!config.isEmpty()) {
			config.put("customizer.class", CMS_JETTY_CUSTOMIZER_CLASS);

			// TODO centralise with Jetty extender
			Object webSocketEnabled = config.get(CmsDeployProperty.WEBSOCKET_ENABLED.getProperty());
			if (webSocketEnabled != null && webSocketEnabled.toString().equals("true")) {
				bc.registerService(ServerEndpointConfig.Configurator.class, new CmsWebSocketConfigurator(), null);
				// config.put(WEBSOCKET_ENABLED, "true");
			}
		}

		int tryCount = 30;
		try {
			tryGettyJetty: while (tryCount > 0) {
				try {
					// FIXME deal with multiple ids
					JettyConfigurator.startServer(CmsConstants.DEFAULT, new Hashtable<>(config));

					Object httpPort = config.get(JettyHttpConstants.HTTP_PORT);
					Object httpsPort = config.get(JettyHttpConstants.HTTPS_PORT);
					log.info(httpPortsMsg(httpPort, httpsPort));

					// Explicitly starts Jetty OSGi HTTP bundle, so that it gets triggered if OSGi
					// configuration is not cleaned
					FrameworkUtil.getBundle(JettyConfigurator.class).start();
					break tryGettyJetty;
				} catch (IllegalStateException e) {
					// e.printStackTrace();
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

	private String httpPortsMsg(Object httpPort, Object httpsPort) {
		return (httpPort != null ? "HTTP " + httpPort + " " : " ") + (httpsPort != null ? "HTTPS " + httpsPort : "");
	}

	/** Override the provided config with the framework properties */
	public Dictionary<String, Object> getHttpServerConfig() {
		String httpPort = getFrameworkProp(CmsDeployProperty.HTTP_PORT);
		String httpsPort = getFrameworkProp(CmsDeployProperty.HTTPS_PORT);
		/// TODO make it more generic
		String httpHost = getFrameworkProp(CmsDeployProperty.HOST);
//		String httpsHost = getFrameworkProp(
//				JettyConfig.JETTY_PROPERTY_PREFIX + CmsHttpConstants.HTTPS_HOST);
		String webSocketEnabled = getFrameworkProp(CmsDeployProperty.WEBSOCKET_ENABLED);

		final Hashtable<String, Object> props = new Hashtable<String, Object>();
		// try {
		if (httpPort != null || httpsPort != null) {
			boolean httpEnabled = httpPort != null;
			props.put(JettyHttpConstants.HTTP_ENABLED, httpEnabled);
			boolean httpsEnabled = httpsPort != null;
			props.put(JettyHttpConstants.HTTPS_ENABLED, httpsEnabled);

			if (httpEnabled) {
				props.put(JettyHttpConstants.HTTP_PORT, httpPort);
				if (httpHost != null)
					props.put(JettyHttpConstants.HTTP_HOST, httpHost);
			}

			if (httpsEnabled) {
				props.put(JettyHttpConstants.HTTPS_PORT, httpsPort);
				if (httpHost != null)
					props.put(JettyHttpConstants.HTTPS_HOST, httpHost);

				props.put(JettyHttpConstants.SSL_KEYSTORETYPE,  getFrameworkProp(CmsDeployProperty.SSL_KEYSTORETYPE));
				props.put(JettyHttpConstants.SSL_KEYSTORE, getFrameworkProp(CmsDeployProperty.SSL_KEYSTORE));
				props.put(JettyHttpConstants.SSL_PASSWORD, getFrameworkProp(CmsDeployProperty.SSL_PASSWORD));

				// client certificate authentication
				String wantClientAuth = getFrameworkProp(CmsDeployProperty.SSL_WANTCLIENTAUTH);
				if (wantClientAuth != null)
					props.put(JettyHttpConstants.SSL_WANTCLIENTAUTH, Boolean.parseBoolean(wantClientAuth));
				String needClientAuth = getFrameworkProp(CmsDeployProperty.SSL_NEEDCLIENTAUTH);
				if (needClientAuth != null)
					props.put(JettyHttpConstants.SSL_NEEDCLIENTAUTH, Boolean.parseBoolean(needClientAuth));
			}

			// web socket
			if (webSocketEnabled != null && webSocketEnabled.equals("true"))
				props.put(CmsDeployProperty.WEBSOCKET_ENABLED.getProperty(), true);

			props.put(CmsConstants.CN, CmsConstants.DEFAULT);
		}
		return props;
	}

	private String getFrameworkProp(CmsDeployProperty deployProperty) {
		return cmsState.getDeployProperty(deployProperty.getProperty());
	}

	public void setCmsState(CmsState cmsState) {
		this.cmsState = cmsState;
	}

}
