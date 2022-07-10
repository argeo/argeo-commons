package org.argeo.cms.jetty;

import java.nio.file.Files;
import java.nio.file.Path;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.websocket.DeploymentException;
import javax.websocket.server.ServerContainer;
import javax.websocket.server.ServerEndpointConfig;

import org.argeo.api.cms.CmsLog;
import org.argeo.api.cms.CmsState;
import org.argeo.cms.CmsDeployProperty;
import org.argeo.cms.websocket.javax.server.CmsWebSocketConfigurator;
import org.argeo.cms.websocket.javax.server.TestEndpoint;
import org.eclipse.jetty.http.UriCompliance;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.server.session.SessionHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.util.thread.QueuedThreadPool;

import org.eclipse.jetty.websocket.javax.server.config.JavaxWebSocketServletContainerInitializer;
import org.eclipse.jetty.websocket.javax.server.config.JavaxWebSocketServletContainerInitializer.Configurator;

public class CmsJettyServer {
	private static final CmsLog log = CmsLog.getLog(CmsJettyServer.class);

	private static final int DEFAULT_IDLE_TIMEOUT = 30000;
	private static final String CONTEXT_TEMPDIR = "javax.servlet.context.tempdir";

	// Equinox compatibility
	private static final String INTERNAL_CONTEXT_CLASSLOADER = "org.eclipse.equinox.http.jetty.internal.ContextClassLoader";

	private Server server;
	private Path tempDir;

	private ServerConnector httpConnector;
	private ServerConnector httpsConnector;

	// WebSocket
	private ServerContainer wsServerContainer;
	private ServerEndpointConfig.Configurator wsEndpointConfigurator;

	private CmsState cmsState;

	public void start() {
		try {
			tempDir = Files.createTempDirectory("jetty");

			server = new Server(new QueuedThreadPool(10, 1));

			configure();
			// context.addServlet(new ServletHolder(new RWTServlet()), "/" + entryPoint);
			// Required to serve rwt-resources. It is important that this is last.
//		ServletHolder holderPwd = new ServletHolder("default", DefaultServlet.class);
//		context.addServlet(holderPwd, "/");

			if (httpConnector != null) {
				httpConnector.open();
				server.addConnector(httpConnector);
			}

			if (httpsConnector != null) {
				httpsConnector.open();
				server.addConnector(httpsConnector);
			}

			// holder

			// context
			ServletContextHandler httpContext = createHttpContext();
			// httpContext.addServlet(holder, "/*");
			addServlets(httpContext);
			enableWebSocket(httpContext);
			server.setHandler(httpContext);

			//
			// START
			server.start();
			//

			Runtime.getRuntime().addShutdownHook(new Thread(() -> stop(), "Jetty shutdown"));

			log.info(httpPortsMsg());
		} catch (Exception e) {
			throw new IllegalStateException("Cannot start Jetty HTTPS server", e);
		}
	}

	protected void addServlets(ServletContextHandler servletContextHandler) throws ServletException {
	}

	public Integer getHttpPort() {
		if (httpConnector == null)
			return null;
		return httpConnector.getLocalPort();
	}

	public Integer getHttpsPort() {
		if (httpsConnector == null)
			return null;
		return httpsConnector.getLocalPort();
	}

	public void stop() {
		try {
			// serverConnector.close();
			server.stop();
			// TODO delete temp dir
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	protected void configure() {
		HttpConfiguration http_config = new HttpConfiguration();

		String httpPortStr = getFrameworkProp(CmsDeployProperty.HTTP_PORT);
		String httpsPortStr = getFrameworkProp(CmsDeployProperty.HTTPS_PORT);

		/// TODO make it more generic
		String httpHost = getFrameworkProp(CmsDeployProperty.HOST);
//		String httpsHost = getFrameworkProp(
//				JettyConfig.JETTY_PROPERTY_PREFIX + CmsHttpConstants.HTTPS_HOST);

		// try {
		if (httpPortStr != null || httpsPortStr != null) {
			boolean httpEnabled = httpPortStr != null;
			// props.put(JettyHttpConstants.HTTP_ENABLED, httpEnabled);
			boolean httpsEnabled = httpsPortStr != null;
			// props.put(JettyHttpConstants.HTTPS_ENABLED, httpsEnabled);
			if (httpsEnabled) {
				int httpsPort = Integer.parseInt(httpsPortStr);
				http_config.setSecureScheme("https");
				http_config.setSecurePort(httpsPort);
			}

			if (httpEnabled) {
				int httpPort = Integer.parseInt(httpPortStr);
				httpConnector = new ServerConnector(server, new HttpConnectionFactory(http_config));
				httpConnector.setPort(httpPort);
				httpConnector.setHost(httpHost);
				httpConnector.setIdleTimeout(DEFAULT_IDLE_TIMEOUT);
			}

			if (httpsEnabled) {

				SslContextFactory.Server sslContextFactory = new SslContextFactory.Server();
				// sslContextFactory.setKeyStore(KeyS)

				sslContextFactory.setKeyStoreType(getFrameworkProp(CmsDeployProperty.SSL_KEYSTORETYPE));
				sslContextFactory.setKeyStorePath(getFrameworkProp(CmsDeployProperty.SSL_KEYSTORE));
				sslContextFactory.setKeyStorePassword(getFrameworkProp(CmsDeployProperty.SSL_PASSWORD));
				// sslContextFactory.setKeyManagerPassword(getFrameworkProp(CmsDeployProperty.SSL_KEYPASSWORD));
				sslContextFactory.setProtocol("TLS");

				sslContextFactory.setTrustStoreType(getFrameworkProp(CmsDeployProperty.SSL_TRUSTSTORETYPE));
				sslContextFactory.setTrustStorePath(getFrameworkProp(CmsDeployProperty.SSL_TRUSTSTORE));
				sslContextFactory.setTrustStorePassword(getFrameworkProp(CmsDeployProperty.SSL_TRUSTSTOREPASSWORD));

				String wantClientAuth = getFrameworkProp(CmsDeployProperty.SSL_WANTCLIENTAUTH);
				if (wantClientAuth != null && wantClientAuth.equals(Boolean.toString(true)))
					sslContextFactory.setWantClientAuth(true);
				String needClientAuth = getFrameworkProp(CmsDeployProperty.SSL_NEEDCLIENTAUTH);
				if (needClientAuth != null && needClientAuth.equals(Boolean.toString(true)))
					sslContextFactory.setNeedClientAuth(true);

				// HTTPS Configuration
				HttpConfiguration https_config = new HttpConfiguration(http_config);
				https_config.addCustomizer(new SecureRequestCustomizer());
				https_config.setUriCompliance(UriCompliance.LEGACY);

				// HTTPS connector
				httpsConnector = new ServerConnector(server, new SslConnectionFactory(sslContextFactory, "http/1.1"),
						new HttpConnectionFactory(https_config));
				int httpsPort = Integer.parseInt(httpsPortStr);
				httpsConnector.setPort(httpsPort);
				httpsConnector.setHost(httpHost);
			}

		}

	}

	protected void enableWebSocket(ServletContextHandler servletContextHandler) {
		String webSocketEnabled = getFrameworkProp(CmsDeployProperty.WEBSOCKET_ENABLED);
		// web socket
		if (webSocketEnabled != null && webSocketEnabled.equals(Boolean.toString(true))) {
			JavaxWebSocketServletContainerInitializer.configure(servletContextHandler, new Configurator() {

				@Override
				public void accept(ServletContext servletContext, ServerContainer serverContainer)
						throws DeploymentException {
					wsServerContainer = serverContainer;

					wsEndpointConfigurator = new CmsWebSocketConfigurator();

					ServerEndpointConfig config = ServerEndpointConfig.Builder
							.create(TestEndpoint.class, "/ws/test/events/").configurator(wsEndpointConfigurator)
							.build();
					try {
						wsServerContainer.addEndpoint(config);
					} catch (DeploymentException e) {
						throw new IllegalStateException("Cannot initalise the WebSocket server runtime.", e);
					}
				}
			});
		}
	}

	protected ServletContextHandler createHttpContext() {
		ServletContextHandler httpContext = new ServletContextHandler();
		httpContext.setAttribute(INTERNAL_CONTEXT_CLASSLOADER, Thread.currentThread().getContextClassLoader());
		httpContext.setClassLoader(this.getClass().getClassLoader());
		httpContext.setContextPath("/");

		httpContext.setAttribute(CONTEXT_TEMPDIR, tempDir.toAbsolutePath().toFile());
		SessionHandler handler = new SessionHandler();
		handler.setMaxInactiveInterval(-1);
		httpContext.setSessionHandler(handler);

		return httpContext;
	}

	private String httpPortsMsg() {

		return (httpConnector != null ? "HTTP " + getHttpPort() + " " : " ")
				+ (httpsConnector != null ? "HTTPS " + getHttpsPort() : "");
	}

	private String getFrameworkProp(CmsDeployProperty deployProperty) {
		return cmsState.getDeployProperty(deployProperty.getProperty());
	}

	public void setCmsState(CmsState cmsState) {
		this.cmsState = cmsState;
	}

}
