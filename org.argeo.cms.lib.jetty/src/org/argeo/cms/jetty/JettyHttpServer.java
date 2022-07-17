package org.argeo.cms.jetty;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

import javax.servlet.ServletException;

import org.argeo.api.cms.CmsLog;
import org.argeo.cms.CmsDeployProperty;
import org.argeo.util.http.HttpServerUtils;
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

public class JettyHttpServer extends HttpsServer {
	private final static CmsLog log = CmsLog.getLog(JettyHttpServer.class);

	private static final int DEFAULT_IDLE_TIMEOUT = 30000;

	private Server server;

	protected ServerConnector httpConnector;
	protected ServerConnector httpsConnector;

	private InetSocketAddress address;

	private ThreadPoolExecutor executor;

	private HttpsConfigurator httpsConfigurator;

	private final Map<String, JettyHttpContext> contexts = new TreeMap<>();

	protected final ContextHandlerCollection contextHandlerCollection = new ContextHandlerCollection();

	private boolean started;

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
			ServletContextHandler rootContextHandler = createRootContextHandler();
			// httpContext.addServlet(holder, "/*");
			if (rootContextHandler != null)
				configureRootContextHandler(rootContextHandler);
//			server.setHandler(rootContextHandler);

			ContextHandlerCollection contextHandlers = new ContextHandlerCollection();
			if (rootContextHandler != null && !contexts.containsKey("/"))
				contextHandlers.addHandler(rootContextHandler);
			for (String contextPath : contexts.keySet()) {
				JettyHttpContext ctx = contexts.get(contextPath);
				contextHandlers.addHandler(ctx.getContextHandler());
			}

			server.setHandler(contextHandlerCollection);

			//
			// START
			server.start();
			//

			Runtime.getRuntime().addShutdownHook(new Thread(() -> stop(), "Jetty shutdown"));

			log.info(httpPortsMsg());
			started = true;
		} catch (Exception e) {
			throw new IllegalStateException("Cannot start Jetty HTTPS server", e);
		}
	}

	@Override
	public void stop(int delay) {
		// TODO wait for processing to complete
		stop();

	}

	public void stop() {
		try {
			// serverConnector.close();
			server.stop();
			// TODO delete temp dir
			started = false;
		} catch (Exception e) {
			e.printStackTrace();
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
		if (contexts.containsKey(path))
			throw new IllegalArgumentException("Context " + path + " already exists");
		JettyHttpContext httpContext = new JettyHttpContext(this, path);
		contexts.put(path, httpContext);

		contextHandlerCollection.addHandler(httpContext.getContextHandler());
		return httpContext;
	}

	@Override
	public synchronized void removeContext(String path) throws IllegalArgumentException {
		if (!contexts.containsKey(path))
			throw new IllegalArgumentException("Context " + path + " does not exist");
		JettyHttpContext httpContext = contexts.remove(path);
		// TODO stop handler first?
		contextHandlerCollection.removeHandler(httpContext.getContextHandler());
	}

	@Override
	public synchronized void removeContext(HttpContext context) {
		removeContext(context.getPath());
	}

	@Override
	public InetSocketAddress getAddress() {
		return address;
	}

	@Override
	public void setHttpsConfigurator(HttpsConfigurator config) {
		this.httpsConfigurator = config;
	}

	@Override
	public HttpsConfigurator getHttpsConfigurator() {
		return httpsConfigurator;
	}

	
	
	protected void configureConnectors() {
		HttpConfiguration httpConfiguration = new HttpConfiguration();

		String httpPortStr = getDeployProperty(CmsDeployProperty.HTTP_PORT);
		String httpsPortStr = getDeployProperty(CmsDeployProperty.HTTPS_PORT);

		/// TODO make it more generic
		String httpHost = getDeployProperty(CmsDeployProperty.HOST);
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
				httpConfiguration.setSecureScheme("https");
				httpConfiguration.setSecurePort(httpsPort);
			}

			if (httpEnabled) {
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
				HttpConfiguration https_config = new HttpConfiguration(httpConfiguration);
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

	protected String getDeployProperty(CmsDeployProperty deployProperty) {
		return System.getProperty(deployProperty.getProperty());
	}

	private String httpPortsMsg() {

		return (httpConnector != null ? "HTTP " + getHttpPort() + " " : " ")
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

	
	public boolean isStarted() {
		return started;
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
