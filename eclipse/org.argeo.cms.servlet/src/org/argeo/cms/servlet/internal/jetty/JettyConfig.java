package org.argeo.cms.servlet.internal.jetty;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
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
import org.argeo.cms.security.PkiUtils;
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
	// Argeo specific
	final static String WEBSOCKET_ENABLED = "websocket.enabled";

	private CmsState cmsState;

	private final BundleContext bc = FrameworkUtil.getBundle(JettyConfig.class).getBundleContext();

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
			Object webSocketEnabled = config.get(WEBSOCKET_ENABLED);
			if (webSocketEnabled != null && webSocketEnabled.toString().equals("true")) {
				bc.registerService(ServerEndpointConfig.Configurator.class, new CmsWebSocketConfigurator(), null);
				config.put(WEBSOCKET_ENABLED, "true");
			}
		}

		int tryCount = 30;
		try {
			tryGettyJetty: while (tryCount > 0) {
				try {
					// FIXME deal with multiple ids
					JettyConfigurator.startServer(CmsConstants.DEFAULT, new Hashtable<>(config));

					Object httpPort = config.get(InternalHttpConstants.HTTP_PORT);
					Object httpsPort = config.get(InternalHttpConstants.HTTPS_PORT);
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
		String httpPort = getFrameworkProp("org.osgi.service.http.port");
		String httpsPort = getFrameworkProp("org.osgi.service.http.port.secure");
		/// TODO make it more generic
		String httpHost = getFrameworkProp(
				InternalHttpConstants.JETTY_PROPERTY_PREFIX + InternalHttpConstants.HTTP_HOST);
		String httpsHost = getFrameworkProp(
				InternalHttpConstants.JETTY_PROPERTY_PREFIX + InternalHttpConstants.HTTPS_HOST);
		String webSocketEnabled = getFrameworkProp(
				InternalHttpConstants.JETTY_PROPERTY_PREFIX + InternalHttpConstants.WEBSOCKET_ENABLED);

		final Hashtable<String, Object> props = new Hashtable<String, Object>();
		// try {
		if (httpPort != null || httpsPort != null) {
			boolean httpEnabled = httpPort != null;
			props.put(InternalHttpConstants.HTTP_ENABLED, httpEnabled);
			boolean httpsEnabled = httpsPort != null;
			props.put(InternalHttpConstants.HTTPS_ENABLED, httpsEnabled);

			if (httpEnabled) {
				props.put(InternalHttpConstants.HTTP_PORT, httpPort);
				if (httpHost != null)
					props.put(InternalHttpConstants.HTTP_HOST, httpHost);
			}

			if (httpsEnabled) {
				props.put(InternalHttpConstants.HTTPS_PORT, httpsPort);
				if (httpsHost != null)
					props.put(InternalHttpConstants.HTTPS_HOST, httpsHost);

				// server certificate
				Path keyStorePath = cmsState.getDataPath(PkiUtils.DEFAULT_KEYSTORE_PATH);
				Path pemKeyPath = cmsState.getDataPath(PkiUtils.DEFAULT_PEM_KEY_PATH);
				Path pemCertPath = cmsState.getDataPath(PkiUtils.DEFAULT_PEM_CERT_PATH);
				String keyStorePasswordStr = getFrameworkProp(
						InternalHttpConstants.JETTY_PROPERTY_PREFIX + InternalHttpConstants.SSL_PASSWORD);
				char[] keyStorePassword;
				if (keyStorePasswordStr == null)
					keyStorePassword = "changeit".toCharArray();
				else
					keyStorePassword = keyStorePasswordStr.toCharArray();

				// if PEM files both exists, update the PKCS12 file
				if (Files.exists(pemCertPath) && Files.exists(pemKeyPath)) {
					// TODO check certificate update time? monitor changes?
					KeyStore keyStore = PkiUtils.getKeyStore(keyStorePath, keyStorePassword, PkiUtils.PKCS12);
					try (Reader key = Files.newBufferedReader(pemKeyPath, StandardCharsets.US_ASCII);
							Reader cert = Files.newBufferedReader(pemCertPath, StandardCharsets.US_ASCII);) {
						PkiUtils.loadPem(keyStore, key, keyStorePassword, cert);
						PkiUtils.saveKeyStore(keyStorePath, keyStorePassword, keyStore);
						if (log.isDebugEnabled())
							log.debug("PEM certificate stored in " + keyStorePath);
					} catch (IOException e) {
						log.error("Cannot read PEM files " + pemKeyPath + " and " + pemCertPath, e);
					}
				}

				if (!Files.exists(keyStorePath))
					PkiUtils.createSelfSignedKeyStore(keyStorePath, keyStorePassword, PkiUtils.PKCS12);
				props.put(InternalHttpConstants.SSL_KEYSTORETYPE, PkiUtils.PKCS12);
				props.put(InternalHttpConstants.SSL_KEYSTORE, keyStorePath.toString());
				props.put(InternalHttpConstants.SSL_PASSWORD, new String(keyStorePassword));

//				props.put(InternalHttpConstants.SSL_KEYSTORETYPE, "PKCS11");
//				props.put(InternalHttpConstants.SSL_KEYSTORE, "../../nssdb");
//				props.put(InternalHttpConstants.SSL_PASSWORD, keyStorePassword);

				// client certificate authentication
				String wantClientAuth = getFrameworkProp(
						InternalHttpConstants.JETTY_PROPERTY_PREFIX + InternalHttpConstants.SSL_WANTCLIENTAUTH);
				if (wantClientAuth != null)
					props.put(InternalHttpConstants.SSL_WANTCLIENTAUTH, Boolean.parseBoolean(wantClientAuth));
				String needClientAuth = getFrameworkProp(
						InternalHttpConstants.JETTY_PROPERTY_PREFIX + InternalHttpConstants.SSL_NEEDCLIENTAUTH);
				if (needClientAuth != null)
					props.put(InternalHttpConstants.SSL_NEEDCLIENTAUTH, Boolean.parseBoolean(needClientAuth));
			}

			// web socket
			if (webSocketEnabled != null && webSocketEnabled.equals("true"))
				props.put(InternalHttpConstants.WEBSOCKET_ENABLED, true);

			props.put(CmsConstants.CN, CmsConstants.DEFAULT);
		}
		return props;
	}

	private String getFrameworkProp(String key) {
		return cmsState.getDeployProperty(key);
	}

	public void setCmsState(CmsState cmsState) {
		this.cmsState = cmsState;
	}

}
