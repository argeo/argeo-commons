package org.argeo.cms.jetty.server;

import java.util.HashMap;
import java.util.Map;

import org.argeo.cms.jetty.AbstractJettyHttpContext;
import org.argeo.cms.jetty.ContextHandlerAttributes;
import org.argeo.cms.jetty.JettyHttpServer;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.handler.ContextHandler;

import com.sun.net.httpserver.HttpContext;

/**
 * A {@link HttpContext} based on pure Jetty server components (no dependency to
 * the jakarta/javax servlet APIs).
 */
public class JettyHttpContext extends AbstractJettyHttpContext {
	private final Handler handler;
	private Map<String, Object> attributes;

	public JettyHttpContext(JettyHttpServer httpServer, String path) {
		super(httpServer, path);
		boolean useContextHandler = false;
		if (useContextHandler) {
			// TODO not working yet
			// (sub contexts do not work)
			handler = new HttpContextJettyContextHandler(this);
			attributes = new ContextHandlerAttributes((ContextHandler) handler);
		} else {
			handler = new HttpContextJettyHandler(this);
			attributes = new HashMap<>();
		}
	}

	@Override
	protected Handler getJettyHandler() {
		return handler;
	}

	@Override
	public Map<String, Object> getAttributes() {
		return attributes;
	}

}
