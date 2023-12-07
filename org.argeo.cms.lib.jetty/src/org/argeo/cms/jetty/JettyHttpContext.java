package org.argeo.cms.jetty;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.argeo.cms.websocket.server.WebsocketEndpoints;
import org.eclipse.jetty.ee8.servlet.ServletContextHandler;

import com.sun.net.httpserver.Authenticator;
import com.sun.net.httpserver.Filter;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

/**
 * An @{HttpContext} implementation based on Jetty. It supports web sockets if
 * the handler implements {@link WebsocketEndpoints}.
 */
abstract class JettyHttpContext extends HttpContext {
	private final JettyHttpServer httpServer;
	private final String path;
	private final List<Filter> filters = new ArrayList<>();

	private HttpHandler handler;
	private Authenticator authenticator;

	public JettyHttpContext(JettyHttpServer httpServer, String path) {
		this.httpServer = httpServer;
		if (!path.endsWith("/"))
			throw new IllegalArgumentException("Path " + path + " should end with a /");
		this.path = path;
	}

	protected abstract ServletContextHandler getServletContextHandler();

	@Override
	public HttpHandler getHandler() {
		return handler;
	}

	@Override
	public void setHandler(HttpHandler handler) {
		if (this.handler != null)
			throw new IllegalArgumentException("Handler is already set");
		Objects.requireNonNull(handler);
		this.handler = handler;
	}

	@Override
	public String getPath() {
		return path;
	}

	@Override
	public HttpServer getServer() {
		return getJettyHttpServer();
	}

	protected JettyHttpServer getJettyHttpServer() {
		return httpServer;
	}

	@Override
	public List<Filter> getFilters() {
		return filters;
	}

	@Override
	public Authenticator setAuthenticator(Authenticator auth) {
		Authenticator previousAuthenticator = authenticator;
		this.authenticator = auth;
		return previousAuthenticator;
	}

	@Override
	public Authenticator getAuthenticator() {
		return authenticator;
	}

}
