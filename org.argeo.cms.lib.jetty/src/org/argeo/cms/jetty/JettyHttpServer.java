package org.argeo.cms.jetty;

import java.io.IOException;
import java.net.InetSocketAddress;
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
import org.eclipse.jetty.http.UriCompliance;
import org.eclipse.jetty.http.pathmap.PathSpec;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.server.handler.PathMappingsHandler;
import org.eclipse.jetty.server.handler.ResourceHandler;
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

import jakarta.websocket.server.ServerContainer;

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

	private final Map<String, AbstractJettyHttpContext> contexts = new TreeMap<>();

	private Handler rootHandler;
	// protected final ContextHandlerCollection contextHandlerCollection = new
	// ContextHandlerCollection(true);
	PathMappingsHandler pathMappingsHandler = new PathMappingsHandler(true);

	private boolean started;

	private CmsState cmsState;
	
	private WebSocketUpgradeHandler webSocketUpgradeHandler;

	@Override
	public void bind(InetSocketAddress addr, int backlog) throws IOException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void start() {
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

			configureConnectors(httpPortStr, httpsPortStr, httpHost);

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
			rootHandler = createRootHandler();
			// httpContext.addServlet(holder, "/*");
			if (rootHandler != null)
				configureRootHandler(rootHandler);

			webSocketUpgradeHandler = WebSocketUpgradeHandler.from(server);
			pathMappingsHandler.addMapping(PathSpec.from("/ws/*"), webSocketUpgradeHandler);

//			if (rootContextHandler != null && !contexts.containsKey("/"))
//				contextHandlerCollection.addHandler(rootContextHandler);
//			server.setHandler(contextHandlerCollection);
			if (rootHandler != null && !contexts.containsKey("/")) {
				pathMappingsHandler.addMapping(PathSpec.from("/"), rootHandler);
			} else {
				ResourceFactory resourceFactory = ResourceFactory.of(server);
				Resource rootResourceDir = resourceFactory.newClassLoaderResource("/static-root/");
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
				httpConnector = new ServerConnector(server, new HttpConnectionFactory(httpConfiguration));
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

				// HTTPS connector
				httpsConnector = new ServerConnector(server, new SslConnectionFactory(sslContextFactory, "http/1.1"),
						new HttpConnectionFactory(httpsConfiguration));
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
		if (!path.endsWith("/"))
			path = path + "/";
		if (contexts.containsKey(path))
			throw new IllegalArgumentException("Context " + path + " already exists");

		AbstractJettyHttpContext httpContext = new JettyHttpContext(this, path);
		contexts.put(path, httpContext);

		Handler jettyHandler = httpContext.getJettyHandler();
		// contextHandlerCollection.addHandler(httpContext.getJettyHandler());
		// FIXME make path more robust
		PathSpec pathSpec = PathSpec.from(path + "*");
		pathMappingsHandler.addMapping(pathSpec, jettyHandler);
		if (isStarted()) {
			// server is already started, handler has to be started explicitly
			try {
				jettyHandler.start();
			} catch (Exception e) {
				throw new IllegalStateException("Could not start dynamically added Jetty handler", e);
			}
		}
		return httpContext;
	}

	@Override
	public synchronized void removeContext(String path) throws IllegalArgumentException {
		if (!path.endsWith("/"))
			path = path + "/";
		if (!contexts.containsKey(path))
			throw new IllegalArgumentException("Context " + path + " does not exist");
		AbstractJettyHttpContext httpContext = contexts.remove(path);
		Handler jettyHandler = httpContext.getJettyHandler();
		if (jettyHandler.isStarted()) {
			try {
				jettyHandler.stop();
			} catch (Exception e) {
				log.error("Cannot stop Jetty handler " + path, e);
			}
		}

//		if (httpContext instanceof ContextHandlerHttpContext contextHandlerHttpContext) {
//			// TODO stop handler first?
//			// FIXME understand compatibility with Jetty 12
//			// contextHandlerCollection.removeHandler(contextHandlerHttpContext.getServletContextHandler());
//		} else {
//			// FIXME apparently servlets cannot be removed in Jetty, we should replace the
//			// handler
//		}
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
		return null;
	}

	protected void configureRootHandler(Handler jettyHandler) {

	}

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

	// TODO protect it?
	public ServerContainer getRootServerContainer() {
		throw new UnsupportedOperationException();
	}

	public Server getServer() {
		return server;
	}

	public WebSocketUpgradeHandler getWebSocketUpgradeHandler() {
		return webSocketUpgradeHandler;
	}

	public static void main(String... args) {
		JettyHttpServer httpServer = new JettyHttpServer();
		System.setProperty("argeo.http.port", "8080");
		httpServer.start();

		httpServer.createContext("/hello", (exchange) -> {
			exchange.getResponseBody().write("Hello World!".getBytes());
		});
		httpServer.createContext("/subcontext", (exchange) -> {
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
