package org.argeo.cms.jetty;

import org.argeo.cms.http.server.AbstractCmsHttpContext;
import org.eclipse.jetty.server.Handler;

/**
 * An @{HttpContext} implementation based on Jetty.
 */
public abstract class AbstractJettyHttpContext extends AbstractCmsHttpContext {
	public AbstractJettyHttpContext(JettyHttpServer httpServer, String path) {
		super(httpServer, path);
	}

	protected abstract Handler getJettyHandler();

	protected JettyServer getJettyHttpServer() {
		return (JettyServer) getServer();
	}

}
