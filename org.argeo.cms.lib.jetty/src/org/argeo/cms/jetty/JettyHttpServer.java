package org.argeo.cms.jetty;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

import javax.servlet.ServletException;
import javax.websocket.server.ServerContainer;

import org.argeo.api.cms.CmsLog;
import org.argeo.api.cms.CmsState;
import org.argeo.cms.CmsDeployProperty;
import org.argeo.cms.http.HttpServerUtils;
import org.eclipse.jetty.http.UriCompliance;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.util.thread.ExecutorThreadPool;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.util.thread.ThreadPool;

import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsServer;

/** An {@link HttpServer} implementation based on Jetty. */
public class JettyHttpServer extends HttpsServer {
	private final static CmsLog log = CmsLog.getLog(JettyHttpServer.class);

	private static final int DEFAULT_IDLE_TIMEOUT = 30000;

	private Server server;

	protected ServerConnector httpConnector;
	protected ServerConnector httpsConnector;

	private InetSocketAddress httpAddress;
	private InetSocketAddress httpsAddress;

	private ThreadPoolExecutor executor;

	private HttpsConfigurator httpsConfigurator;

	private final Map<String, JettyHttpContext> contexts = new TreeMap<>();

	private ServletContextHandler rootContextHandler;
	protected final ContextHandlerCollection contextHandlerCollection = new ContextHandlerCollection();

	private boolean started;

	private CmsState cmsState;

	@Override
	public void bind(InetSocketAddress addr, int backlog) throws IOException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void start() {
		try {

			ThreadPool threadPool = null;
			if (executor != null) {
				threadPool = new ExecutorThreadPool(executor);
			} else {
				// TODO make it configurable
				threadPool = new QueuedThreadPool(10, 1);
			}

			server = new Server(threadPool);

			configureConnectors();

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
			rootContextHandler = createRootContextHandler();
			// httpContext.addServlet(holder, "/*");
			if (rootContextHandler != null)
				configureRootContextHandler(rootContextHandler);

			if (rootContextHandler != null && !contexts.containsKey("/"))
				contextHandlerCollection.addHandler(rootContextHandler);

			server.setHandler(contextHandlerCollection);

			//
			// START
			server.start();
			//

			// Addresses
			String httpHost = getDeployProperty(CmsDeployProperty.HOST);
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

	protected void configureConnectors() {
		String httpPortStr = getDeployProperty(CmsDeployProperty.HTTP_PORT);
		String httpsPortStr = getDeployProperty(CmsDeployProperty.HTTPS_PORT);
		if (httpPortStr != null && httpsPortStr != null)
			throw new IllegalArgumentException("Either an HTTP or an HTTPS port should be configured, not both");
		if (httpPortStr == null && httpsPortStr == null)
			throw new IllegalArgumentException("Neither an HTTP or HTTPS port was configured");

		/// TODO make it more generic
		String httpHost = getDeployProperty(CmsDeployProperty.HOST);

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

		JettyHttpContext httpContext = new ServletHttpContext(this, path);
		contexts.put(path, httpContext);

		contextHandlerCollection.addHandler(httpContext.getServletContextHandler());
		return httpContext;
	}

	@Override
	public synchronized void removeContext(String path) throws IllegalArgumentException {
		if (!contexts.containsKey(path))
			throw new IllegalArgumentException("Context " + path + " does not exist");
		JettyHttpContext httpContext = contexts.remove(path);
		if (httpContext instanceof ContextHandlerHttpContext contextHandlerHttpContext) {
			// TODO stop handler first?
			contextHandlerCollection.removeHandler(contextHandlerHttpContext.getServletContextHandler());
		}
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

		return (httpConnector != null ? "HTTP " + getHttpPort() + " " : "")
				+ (httpsConnector != null ? "HTTPS " + getHttpsPort() : "");
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

	protected ServletContextHandler createRootContextHandler() {
		return null;
	}

	protected void configureRootContextHandler(ServletContextHandler servletContextHandler) throws ServletException {

	}

	public void setCmsState(CmsState cmsState) {
		this.cmsState = cmsState;
	}

	boolean isStarted() {
		return started;
	}

	ServletContextHandler getRootContextHandler() {
		return rootContextHandler;
	}

	ServerContainer getRootServerContainer() {
		throw new UnsupportedOperationException();
	}

	public static void main(String... args) {
		JettyHttpServer httpServer = new JettyHttpServer();
		System.setProperty("argeo.http.port", "8080");
		httpServer.createContext("/", (exchange) -> {
			exchange.getResponseBody().write("Hello World!".getBytes());
		});
		httpServer.start();
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
