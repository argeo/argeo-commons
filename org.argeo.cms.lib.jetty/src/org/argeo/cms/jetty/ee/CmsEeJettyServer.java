package org.argeo.cms.jetty.ee;

import java.util.Map;

import org.argeo.cms.jetty.CmsJettyServer;
import org.argeo.cms.jetty.JettyHttpServer;
import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.ee10.servlet.SessionHandler;
import org.eclipse.jetty.http.pathmap.PathSpec;

/** A {@link JettyHttpServer} which is compatible with Equinox servlets. */
public class CmsEeJettyServer extends CmsJettyServer {
	private final static int DEFAULT_MAX_INACTIVE_INTERVAL = 24 * 60 * 60;

//	private static final String CONTEXT_TEMPDIR = "javax.servlet.context.tempdir";
//	// Equinox compatibility
//	private static final String INTERNAL_CONTEXT_CLASSLOADER = "org.eclipse.equinox.http.jetty.internal.ContextClassLoader";
//	private Path tempDir;

//	private CompletableFuture<ServerContainer> serverContainer = new CompletableFuture<>();

	protected void addServlets(ServletContextHandler servletContextHandler) {
	}

//	@Override
//	public void start() {
//		try {
//			// TODO make it more robust
//			tempDir = Files.createTempDirectory("jetty");
//		} catch (IOException e) {
//			throw new IllegalStateException("Cannot create temp dir", e);
//		}
//		super.start();
//	}

//	@Override
//	protected ServletContextHandler createRootHandler() {
//		ServletContextHandler servletContextHandler = new ServletContextHandler();
//		servletContextHandler.setAttribute(INTERNAL_CONTEXT_CLASSLOADER,
//				Thread.currentThread().getContextClassLoader());
//		servletContextHandler.setClassLoader(this.getClass().getClassLoader());
//		servletContextHandler.setContextPath("/");
//		// servletContextHandler.setContextPath("/cms/user");
//
//		servletContextHandler.setAttribute(CONTEXT_TEMPDIR, tempDir.toAbsolutePath().toFile());
//		SessionHandler handler = new SessionHandler();
//		// TODO make it configurable
//		handler.setMaxInactiveInterval(-1);
//		servletContextHandler.setSessionHandler(handler);
//
//		JakartaWebSocketServletContainerInitializer.configure(servletContextHandler, new Configurator() {
//
//			@Override
//			public void accept(ServletContext servletContext, ServerContainer serverContainer)
//					throws DeploymentException {
//				CmsJettyServer.this.serverContainer.complete(serverContainer);
//			}
//		});
//
//		return servletContextHandler;
//	}

	public void addJakartaServletContextHandler(ServletContextHandler servletContextHandler,
			Map<String, String> properties) {
		// servletContextHandler.setClassLoader(this.getClass().getClassLoader());

		SessionHandler sessionHandler = new SessionHandler();
		// Make sure servlet sessions are integrated with plain Jetty sessions
		sessionHandler.setSessionPath("/");
		sessionHandler.setSessionIdManager(getSessionIdManager());
		// TODO make it configurable
		sessionHandler.setMaxInactiveInterval(DEFAULT_MAX_INACTIVE_INTERVAL);
		servletContextHandler.setSessionHandler(sessionHandler);

		String contextPath = servletContextHandler.getContextPath();

		getPathMappingsHandler().addMapping(PathSpec.from(contextPath + (!contextPath.endsWith("/") ? "/" : "") + "*"),
				servletContextHandler);
		if (getServer() != null && getServer().isStarted()) {
			// server is already started, handler has to be started explicitly
			// but after mapping it otherwise implicit setServer fails.
			try {
				servletContextHandler.start();
			} catch (Exception e) {
				throw new IllegalStateException("Could not start dynamically added Jetty handler", e);
			}
		}
		getPathMappingsHandler().manage(servletContextHandler);// so that it is stopped when removed
	}

	public void removeJakartaServletContextHandler(ServletContextHandler servletContextHandler,
			Map<String, String> properties) {
		// TODO unregister servlet context
	}

	@Deprecated
	public void addJavaxServletContextHandler(org.eclipse.jetty.ee8.servlet.ServletContextHandler servletContextHandler,
			Map<String, String> properties) {
		// servletContextHandler.setClassLoader(this.getClass().getClassLoader());

		org.eclipse.jetty.ee8.nested.SessionHandler sessionHandler = new org.eclipse.jetty.ee8.nested.SessionHandler();
		// Make sure servlet sessions are integrated with plain Jetty sessions
		sessionHandler.setSessionPath("/");
		sessionHandler.setSessionIdManager(getSessionIdManager());
		// TODO make it configurable
		sessionHandler.setMaxInactiveInterval(DEFAULT_MAX_INACTIVE_INTERVAL);
		servletContextHandler.setSessionHandler(sessionHandler);

		String contextPath = servletContextHandler.getContextPath();

		getPathMappingsHandler().addMapping(PathSpec.from(contextPath + (!contextPath.endsWith("/") ? "/" : "") + "*"),
				servletContextHandler.get());
		if (getServer() != null && getServer().isStarted()) {
			// server is already started, handler has to be started explicitly
			// but after mapping it otherwise implicit setServer fails.
			try {
				servletContextHandler.start();
			} catch (Exception e) {
				throw new IllegalStateException("Could not start dynamically added Jetty handler", e);
			}
		}
//		getPathMappingsHandler().manage(servletContextHandler);// so that it is stopped when removed
	}

	@Deprecated
	public void removeJavaxServletContextHandler(
			org.eclipse.jetty.ee8.servlet.ServletContextHandler servletContextHandler, Map<String, String> properties) {
		// TODO unregister servlet context
	}

//	@Override
//	public ServerContainer getRootServerContainer() {
//		return serverContainer.join();
//	}

}
