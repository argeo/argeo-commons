package org.argeo.cms.jetty;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

import javax.net.ssl.SSLContext;

import org.argeo.api.cms.CmsLog;
import org.argeo.api.cms.CmsState;
import org.argeo.cms.CmsDeployProperty;
import org.argeo.cms.http.server.HttpServerUtils;
import org.argeo.cms.jetty.server.JettyHttpContext;
import org.eclipse.jetty.alpn.java.server.JDK9ServerALPNProcessor;
import org.eclipse.jetty.alpn.server.ALPNServerConnectionFactory;
import org.eclipse.jetty.http.PreEncodedHttpField;
import org.eclipse.jetty.http.UriCompliance;
import org.eclipse.jetty.http.pathmap.PathSpec;
import org.eclipse.jetty.http2.hpack.HpackFieldPreEncoder;
import org.eclipse.jetty.http2.server.HTTP2CServerConnectionFactory;
import org.eclipse.jetty.http2.server.HTTP2ServerConnectionFactory;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.server.handler.PathMappingsHandler;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.session.DefaultSessionIdManager;
import org.eclipse.jetty.session.HouseKeeper;
import org.eclipse.jetty.session.SessionIdManager;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.util.resource.ResourceFactory;
import org.eclipse.jetty.util.resource.Resources;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.util.thread.ExecutorThreadPool;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.util.thread.ThreadPool;
import org.eclipse.jetty.websocket.server.WebSocketUpgradeHandler;

import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsServer;

/** An {@link HttpServer} implementation based on Jetty. */
public class JettyHttpServer extends HttpsServer {
	private final static CmsLog log = CmsLog.getLog(JettyHttpServer.class);

	/** Long timeout since our users may have poor connections. */
	private static final int DEFAULT_IDLE_TIMEOUT = 120 * 1000;

	private Server server;

	protected ServerConnector httpConnector;
	protected ServerConnector httpsConnector;

	private InetSocketAddress httpAddress;
	private InetSocketAddress httpsAddress;

	private ThreadPoolExecutor executor;

	private HttpsConfigurator httpsConfigurator;

	// We have to track contexts in order to honour the removeContext() API
	private final Map<String, AbstractJettyHttpContext> contexts = new TreeMap<>();

	private SessionIdManager sessionIdManager;
	private Handler rootHandler;
//	protected final ContextHandlerCollection contextHandlerCollection = new ContextHandlerCollection(true);
	private PathMappingsHandler pathMappingsHandler = new PathMappingsHandler(true);

	private boolean started;

	private CmsState cmsState;

	private WebSocketUpgradeHandler webSocketUpgradeHandler;

	@Override
	public void bind(InetSocketAddress addr, int backlog) throws IOException {
		// TODO implement multiple connectors
		throw new UnsupportedOperationException();
	}

	@Override
	public void start() {
		if (cmsState == null) {
			log.warn("No CMS state is configured, make sure to configure via ystem properties.");
		}
		String httpPortStr = getDeployProperty(CmsDeployProperty.HTTP_PORT);
		String httpsPortStr = getDeployProperty(CmsDeployProperty.HTTPS_PORT);
		if (httpPortStr != null && httpsPortStr != null)
			throw new IllegalArgumentException("Either an HTTP or an HTTPS port should be configured, not both");
		if (httpPortStr == null && httpsPortStr == null) {
			log.warn("Neither an HTTP or an HTTPS port was configured, not starting Jetty");
		}

		/// TODO make it more generic
		String httpHost = getDeployProperty(CmsDeployProperty.HOST);

		try {

			ThreadPool threadPool = null;
			if (executor != null) {
				threadPool = new ExecutorThreadPool(executor);
			} else {
				// TODO make it configurable
				threadPool = new QueuedThreadPool(10, 1);
			}

			server = new Server(threadPool);

			// Session management common to all handlers
			DefaultSessionIdManager idMgr = new DefaultSessionIdManager(server);
			// TODO deal with clustering
			// idMgr.setWorkerName("server7");
			server.addBean(idMgr, true);
			sessionIdManager = idMgr;

			HouseKeeper houseKeeper = new HouseKeeper();
			houseKeeper.setSessionIdManager(idMgr);
			// set the frequency of scavenge cycles
			houseKeeper.setIntervalSec(600L);
			idMgr.setSessionHouseKeeper(houseKeeper);

			// Connectors configuration
			configureConnectors(httpPortStr, httpsPortStr, httpHost);
			if (httpConnector != null) {
				httpConnector.open();
				server.addConnector(httpConnector);
			}
			if (httpsConnector != null) {
				httpsConnector.open();
				server.addConnector(httpsConnector);
			}

			// root handler
			rootHandler = createRootHandler();
			// httpContext.addServlet(holder, "/*");
//			if (rootHandler != null)
//				configureRootHandler(rootHandler);

//			if (rootHandler != null && !contexts.containsKey("/"))
//				contextHandlerCollection.addHandler(rootHandler);
//			// TODO add top-levle handler (compression, QoS, etc.)
//			server.setHandler(contextHandlerCollection);

			webSocketUpgradeHandler = WebSocketUpgradeHandler.from(server);
//			ContextHandler webSocketContextHandler = new ContextHandler(webSocketUpgradeHandler, "/ws");
//			contextHandlerCollection.addHandler(webSocketContextHandler);
			pathMappingsHandler.addMapping(PathSpec.from("/ws/*"), webSocketUpgradeHandler);

			if (rootHandler != null) {
				pathMappingsHandler.addMapping(PathSpec.from("/"), rootHandler);
			} else {
				ResourceFactory resourceFactory = ResourceFactory.of(server);
//				Resource rootResourceDir = resourceFactory.newClassLoaderResource("/static-root/");
				Resource rootResourceDir = resourceFactory.newResource(Paths.get("/var/www/html"));

				if (!Resources.isReadableDirectory(rootResourceDir))
					throw new IllegalStateException("Unable to find root resource");

				ResourceHandler rootResourceHandler = new ResourceHandler();
				rootResourceHandler.setBaseResource(rootResourceDir);
				rootResourceHandler.setDirAllowed(false);
				rootResourceHandler.setWelcomeFiles("index.html");

				pathMappingsHandler.addMapping(PathSpec.from("/"), rootResourceHandler);
			}
			server.setHandler(pathMappingsHandler);

			//
			// START
			server.start();
			//

			// Addresses
			String fallBackHostname = cmsState != null ? cmsState.getHostname() : "::1";
			if (httpConnector != null) {
				httpAddress = new InetSocketAddress(httpHost != null ? httpHost : fallBackHostname,
						httpConnector.getLocalPort());
			} else if (httpsConnector != null) {
				httpsAddress = new InetSocketAddress(httpHost != null ? httpHost : fallBackHostname,
						httpsConnector.getLocalPort());
			}
			// Clean up
			Runtime.getRuntime().addShutdownHook(new Thread(() -> stop(), "Jetty shutdown"));

			log.info(httpPortsMsg());
			started = true;
		} catch (Exception e) {
			stop();
			throw new IllegalStateException("Cannot start Jetty HTTP server", e);
		}
	}

	protected void configureConnectors(String httpPortStr, String httpsPortStr, String httpHost) {

		// try {
		if (httpPortStr != null || httpsPortStr != null) {
			// TODO deal with hostname resolving taking too much time
//			String fallBackHostname = InetAddress.getLocalHost().getHostName();

			boolean httpEnabled = httpPortStr != null;
			boolean httpsEnabled = httpsPortStr != null;

			if (httpEnabled) {
				HttpConfiguration httpConfiguration = new HttpConfiguration();

				if (httpsEnabled) {// not supported anymore to have both http and https, but it may change again
					int httpsPort = Integer.parseInt(httpsPortStr);
					httpConfiguration.setSecureScheme("https");
					httpConfiguration.setSecurePort(httpsPort);
				}

				int httpPort = Integer.parseInt(httpPortStr);

				// see
				// https://jetty.org/docs/jetty/12/programming-guide/server/http.html#connector-protocol-http2
				// HTTP/1.1
				HttpConnectionFactory http11 = new HttpConnectionFactory(httpConfiguration);
				// HTTP/2 PLAIN (h2c)
				HTTP2CServerConnectionFactory h2c = new HTTP2CServerConnectionFactory(httpConfiguration);

				httpConnector = new ServerConnector(server, http11, h2c);
				httpConnector.setPort(httpPort);
				httpConnector.setHost(httpHost);
				httpConnector.setIdleTimeout(DEFAULT_IDLE_TIMEOUT);

			}

			if (httpsEnabled) {
				if (httpsConfigurator == null) {
					// we make sure that an HttpSConfigurator is set, so that clients can detect
					// whether this server is HTTP or HTTPS
					try {
						httpsConfigurator = new HttpsConfigurator(SSLContext.getDefault());
					} catch (NoSuchAlgorithmException e) {
						throw new IllegalStateException("Cannot initalise SSL Context", e);
					}
				}

				SslContextFactory.Server sslContextFactory = new SslContextFactory.Server();
				// sslContextFactory.setKeyStore(KeyS)

				// FIXME check values and warn/fail when mandatory ones are not set
				sslContextFactory.setKeyStoreType(getDeployProperty(CmsDeployProperty.SSL_KEYSTORETYPE));
				sslContextFactory.setKeyStorePath(getDeployProperty(CmsDeployProperty.SSL_KEYSTORE));
				sslContextFactory.setKeyStorePassword(getDeployProperty(CmsDeployProperty.SSL_PASSWORD));
				// sslContextFactory.setKeyManagerPassword(getFrameworkProp(CmsDeployProperty.SSL_KEYPASSWORD));
				sslContextFactory.setProtocol("TLS");

				sslContextFactory.setTrustStoreType(getDeployProperty(CmsDeployProperty.SSL_TRUSTSTORETYPE));
				sslContextFactory.setTrustStorePath(getDeployProperty(CmsDeployProperty.SSL_TRUSTSTORE));
				sslContextFactory.setTrustStorePassword(getDeployProperty(CmsDeployProperty.SSL_TRUSTSTOREPASSWORD));

				String wantClientAuth = getDeployProperty(CmsDeployProperty.SSL_WANTCLIENTAUTH);
				if (wantClientAuth != null && wantClientAuth.equals(Boolean.toString(true)))
					sslContextFactory.setWantClientAuth(true);
				String needClientAuth = getDeployProperty(CmsDeployProperty.SSL_NEEDCLIENTAUTH);
				if (needClientAuth != null && needClientAuth.equals(Boolean.toString(true)))
					sslContextFactory.setNeedClientAuth(true);

				// HTTPS Configuration
				HttpConfiguration httpsConfiguration = new HttpConfiguration();
				httpsConfiguration.addCustomizer(new SecureRequestCustomizer());
				httpsConfiguration.setUriCompliance(UriCompliance.LEGACY);

				// see
				// https://jetty.org/docs/jetty/12/programming-guide/server/http.html#connector-protocol-http2-tls
				// HTTP/1.1
				HttpConnectionFactory http11 = new HttpConnectionFactory(httpsConfiguration);
				boolean http2 = true;
				if (http2) {
					// HTTP/2 over TLS (h2)
					HTTP2ServerConnectionFactory h2 = new HTTP2ServerConnectionFactory(httpsConfiguration);
					// ALPN protocol for security negotiation
					ALPNServerConnectionFactory alpn = null;
					// BEGIN HACK
					// we make sure that the proper class loader is used to load the processor
					// implementation
					ClassLoader currentContextCL = Thread.currentThread().getContextClassLoader();
					try {
						Thread.currentThread().setContextClassLoader(JDK9ServerALPNProcessor.class.getClassLoader());
						alpn = new ALPNServerConnectionFactory();
					} finally {
						Thread.currentThread().setContextClassLoader(currentContextCL);
					}
					// END HACK

					// The default protocol to use in case there is no negotiation.
					alpn.setDefaultProtocol(http11.getProtocol());

					SslConnectionFactory tls = new SslConnectionFactory(sslContextFactory, alpn.getProtocol());

					// HTTPS connector
					httpsConnector = new ServerConnector(server, tls, alpn, h2, http11);
				} else {
					SslConnectionFactory tls = new SslConnectionFactory(sslContextFactory, "http/1.1");
					httpsConnector = new ServerConnector(server, tls, http11);
				}
				int httpsPort = Integer.parseInt(httpsPortStr);
				httpsConnector.setPort(httpsPort);
				httpsConnector.setHost(httpHost);
				httpsConnector.setIdleTimeout(DEFAULT_IDLE_TIMEOUT);
			}
		}
	}

	@Override
	public void stop(int delay) {
		// TODO wait for processing to complete
		stop();

	}

	public void stop() {
		try {
			server.stop();
			// TODO delete temp dir
			started = false;
			log.debug(() -> "Stopped Jetty server");
		} catch (Exception e) {
			log.error("Cannot stop Jetty HTTP server", e);
		}

	}

	@Override
	public void setExecutor(Executor executor) {
		if (!(executor instanceof ThreadPoolExecutor))
			throw new IllegalArgumentException("Only " + ThreadPoolExecutor.class.getName() + " are supported");
		this.executor = (ThreadPoolExecutor) executor;
	}

	@Override
	public Executor getExecutor() {
		return executor;
	}

	@Override
	public synchronized HttpContext createContext(String path, HttpHandler handler) {
		HttpContext httpContext = createContext(path);
		httpContext.setHandler(handler);
		return httpContext;
	}

	@Override
	public synchronized HttpContext createContext(String path) {
//		if (!path.endsWith("/"))
//			path = path + "/";
//		if (contexts.containsKey(path))
//			throw new IllegalArgumentException("Context " + path + " already exists");

		AbstractJettyHttpContext httpContext = new JettyHttpContext(this, path);
		contexts.put(path, httpContext);

		Handler jettyHandler = httpContext.getJettyHandler();
		// IMPORTANT: server need to be set on this handler tree before it is added
//		jettyHandler.setServer(getServer());

		// contextHandlerCollection.addHandler(httpContext.getJettyHandler());

		PathSpec pathSpec = PathSpec.from(path + (path.endsWith("/") ? "*" : ""));
		pathMappingsHandler.addMapping(pathSpec, jettyHandler);
		if (server.isStarted()) {
			// server is already started, handler has to be started explicitly
			// but after mapping it otherwise implicit setServer fails.
			try {
				jettyHandler.start();
			} catch (Exception e) {
				throw new IllegalStateException("Could not start dynamically added Jetty handler", e);
			}
		}
		pathMappingsHandler.manage(jettyHandler);// so that it is stopped when removed
		return httpContext;
	}

	@Override
	public synchronized void removeContext(String path) throws IllegalArgumentException {
//		if (!path.endsWith("/"))
//			path = path + "/";
		if (!contexts.containsKey(path))
			throw new IllegalArgumentException("Context " + path + " does not exist");
		contexts.remove(path);
//		Handler jettyHandler = httpContext.getJettyHandler();
//		if (jettyHandler.isStarted()) {
//			try {
//				jettyHandler.stop();
//			} catch (Exception e) {
//				log.error("Cannot stop Jetty handler " + path, e);
//			}
//		}

		// this will unregister the previous handler
		Handler noOpHandler = new DefaultHandler(false, false);
		pathMappingsHandler.addMapping(PathSpec.from(path + (path.endsWith("/") ? "*" : "")), noOpHandler);
		if (server.isStarted()) {
			// server is already started, handler has to be started explicitly
			try {
				noOpHandler.start();
			} catch (Exception e) {
				throw new IllegalStateException("Could not start dynamically added Jetty handler", e);
			}
		}
		pathMappingsHandler.manage(noOpHandler);
	}

	@Override
	public synchronized void removeContext(HttpContext context) {
		removeContext(context.getPath());
	}

	@Override
	public InetSocketAddress getAddress() {
		InetSocketAddress res = httpAddress != null ? httpAddress : httpsAddress;
		if (res == null)
			throw new IllegalStateException("Neither an HTTP nor and HTTPS address is available");
		return res;
	}

	@Override
	public void setHttpsConfigurator(HttpsConfigurator config) {
		this.httpsConfigurator = config;
	}

	@Override
	public HttpsConfigurator getHttpsConfigurator() {
		return httpsConfigurator;
	}

	protected String getDeployProperty(CmsDeployProperty deployProperty) {
		return cmsState != null ? cmsState.getDeployProperty(deployProperty.getProperty())
				: System.getProperty(deployProperty.getProperty());
	}

	private String httpPortsMsg() {
		String hostStr = getHost();
		hostStr = hostStr == null ? "*:" : hostStr + ":";
		return (httpConnector != null ? "# HTTP " + hostStr + getHttpPort() + " " : "")
				+ (httpsConnector != null ? "# HTTPS " + hostStr + getHttpsPort() : "");
	}

	public String getHost() {
		if (httpConnector == null)
			return null;
		return httpConnector.getHost();
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

	protected Handler createRootHandler() {
		return new DefaultHandler(false, false);
	}

//	protected void configureRootHandler(Handler jettyHandler) {
//
//	}

	// TODO protect it?
	public Handler getRootHandler() {
		return rootHandler;
	}

	public void setCmsState(CmsState cmsState) {
		this.cmsState = cmsState;
	}

	public boolean isStarted() {
		return started;
	}

//	public ServerContainer getRootServerContainer() {
//		throw new UnsupportedOperationException();
//	}

	public Server getServer() {
		return server;
	}

	protected PathMappingsHandler getPathMappingsHandler() {
		return pathMappingsHandler;
	}

	public WebSocketUpgradeHandler getWebSocketUpgradeHandler() {
		return webSocketUpgradeHandler;
	}

	protected SessionIdManager getSessionIdManager() {
		return sessionIdManager;
	}

	static {
		ClassLoader currentContextCL = Thread.currentThread().getContextClassLoader();
		// BEGIN HACK
		// Force initialisation of pre-field encoder for HTTP/2
		// this could be done by wrapping new Server() instead,
		// but it may have other side effects
		try {
			// services are loaded in the static initialisation of PreEncodedHttpField
			// since HTTP/1 and HTTP/1.1. are forced, we just make sure HTTP/2 hpack will be
			// considered
			Thread.currentThread().setContextClassLoader(HpackFieldPreEncoder.class.getClassLoader());
			new PreEncodedHttpField("Hack", "HACK");
		} finally {
			Thread.currentThread().setContextClassLoader(currentContextCL);
		}
		// END HACK
	}

	public static void main(String... args) {
		JettyHttpServer httpServer = new JettyHttpServer();
		System.setProperty("argeo.http.port", "8080");
		httpServer.start();

		httpServer.createContext("/hello", (exchange) -> {
			exchange.getResponseBody().write("Hello World!".getBytes());
		});
		httpServer.createContext("/sub/context", (exchange) -> {
			final String key = "count";
			Integer count = (Integer) exchange.getHttpContext().getAttributes().get(key);
			if (count == null)
				exchange.getHttpContext().getAttributes().put(key, 0);
			else
				exchange.getHttpContext().getAttributes().put(key, count + 1);
			StringBuilder sb = new StringBuilder();
			sb.append("Subcontext:");
			sb.append(" " + key + "=" + exchange.getHttpContext().getAttributes().get(key));
			sb.append(" relativePath=" + HttpServerUtils.relativize(exchange));
			exchange.getResponseBody().write(sb.toString().getBytes());
		});
	}
}
