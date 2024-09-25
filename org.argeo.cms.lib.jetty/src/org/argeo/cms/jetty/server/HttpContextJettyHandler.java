package org.argeo.cms.jetty.server;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Handler.Abstract;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;

import com.sun.net.httpserver.Filter;
import com.sun.net.httpserver.HttpContext;

/** A Jetty {@link Handler} based on an {@link HttpContext}. */
class HttpContextJettyHandler extends Abstract {
	private final HttpContext httpContext;

	public HttpContextJettyHandler(HttpContext httpContext) {
		this.httpContext = httpContext;
	}

	@Override
	public boolean handle(Request request, Response response, Callback callback) throws Exception {
		JettyHttpExchange httpExchange = new JettyHttpExchange(httpContext, request, response);
		
		Filter.Chain chain = new Filter.Chain(httpContext.getFilters(), httpContext.getHandler());
		chain.doFilter(httpExchange);
//		// FIXME deal with authentication
//		httpContext.getHandler().handle(httpExchange);
		callback.succeeded();
		return true;
	}

}
