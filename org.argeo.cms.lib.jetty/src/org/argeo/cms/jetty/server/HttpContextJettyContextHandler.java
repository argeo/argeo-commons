package org.argeo.cms.jetty.server;

import org.eclipse.jetty.server.handler.ContextHandler;

import com.sun.net.httpserver.HttpContext;

/** A Jetty {@link ContextHandler} based on an {@link HttpContext}. */
class HttpContextJettyContextHandler extends ContextHandler {

	public HttpContextJettyContextHandler(HttpContext httpContext) {
		// FIXME make path more robust
		super(new HttpContextJettyHandler(httpContext), httpContext.getPath() + "/*");
	}

}
