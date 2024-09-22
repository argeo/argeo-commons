package org.argeo.cms.jetty.server;

import java.util.Map;

import org.argeo.cms.jetty.AbstractJettyHttpContext;
import org.argeo.cms.jetty.ContextHandlerAttributes;
import org.argeo.cms.jetty.JettyHttpServer;
import org.eclipse.jetty.server.handler.ContextHandler;

import com.sun.net.httpserver.HttpContext;

/**
 * A {@link HttpContext} based on pure Jetty server components (no dependency to
 * the jakarta/javax servlet APIs).
 */
public class JettyHttpContext extends AbstractJettyHttpContext {
	private final ContextHandler contextHandler;
	private final ContextHandlerAttributes attributes;

	public JettyHttpContext(JettyHttpServer httpServer, String path) {
		super(httpServer, path);
		contextHandler = new HttpContextJettyContextHandler(this);
		attributes = new ContextHandlerAttributes(contextHandler);
	}

	@Override
	protected ContextHandler getJettyHandler() {
		return contextHandler;
	}

	@Override
	public Map<String, Object> getAttributes() {
		return attributes;
	}

}
