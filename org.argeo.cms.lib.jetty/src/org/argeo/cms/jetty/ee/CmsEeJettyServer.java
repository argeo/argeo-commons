package org.argeo.cms.jetty.ee;

import java.util.Map;

import org.argeo.cms.jetty.CmsJettyServer;
import org.argeo.cms.jetty.JettyHttpServer;
import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.ee10.servlet.SessionHandler;
import org.eclipse.jetty.http.pathmap.PathSpec;

/** A {@link JettyHttpServer} which is compatible with Equinox servlets. */
public class CmsEeJettyServer extends CmsJettyServer {
	protected final static int DEFAULT_MAX_INACTIVE_INTERVAL = 24 * 60 * 60;

	protected void addServlets(ServletContextHandler servletContextHandler) {
	}

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
		if (get() != null && get().isStarted()) {
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
}
