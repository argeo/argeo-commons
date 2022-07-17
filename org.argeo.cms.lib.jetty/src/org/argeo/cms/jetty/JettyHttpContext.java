package org.argeo.cms.jetty;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.argeo.cms.servlet.httpserver.HttpContextServlet;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.session.SessionHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import com.sun.net.httpserver.Authenticator;
import com.sun.net.httpserver.Filter;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

/** Trivial implementation of @{HttpContext}. */
class JettyHttpContext extends HttpContext {
	private final JettyHttpServer httpServer;
	private final String path;
	private final ContextHandler contextHandler;
	private final ContextAttributes attributes;
	private final List<Filter> filters = new ArrayList<>();

	private HttpHandler handler;
	private Authenticator authenticator;

	public JettyHttpContext(JettyHttpServer httpServer, String path) {
		this.httpServer = httpServer;
		this.path = path;

		// Jetty context handler
		ServletContextHandler servletContextHandler = new ServletContextHandler();
		servletContextHandler.setContextPath(path);
		HttpContextServlet servlet = new HttpContextServlet(this);
		servletContextHandler.addServlet(new ServletHolder(servlet), "/*");
		SessionHandler sessionHandler = new SessionHandler();
		// FIXME find a better default
		sessionHandler.setMaxInactiveInterval(-1);
		servletContextHandler.setSessionHandler(sessionHandler);
		contextHandler = servletContextHandler;

		attributes = new ContextAttributes();
	}

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

		if (httpServer.isStarted())
			try {
				contextHandler.start();
			} catch (Exception e) {
				throw new IllegalStateException("Cannot start context handler", e);
			}
	}

	@Override
	public String getPath() {
		return path;
	}

	@Override
	public HttpServer getServer() {
		return httpServer;
	}

	@Override
	public Map<String, Object> getAttributes() {
		return attributes;
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

	public Handler getContextHandler() {
		return contextHandler;
	}

	private class ContextAttributes extends AbstractMap<String, Object> {
		@Override
		public Set<Entry<String, Object>> entrySet() {
			Set<Entry<String, Object>> entries = new HashSet<>();
			for (Enumeration<String> keys = contextHandler.getAttributeNames(); keys.hasMoreElements();) {
				entries.add(new ContextAttributeEntry(keys.nextElement()));
			}
			return entries;
		}

		@Override
		public Object put(String key, Object value) {
			Object previousValue = get(key);
			contextHandler.setAttribute(key, value);
			return previousValue;
		}

		private class ContextAttributeEntry implements Map.Entry<String, Object> {
			private final String key;

			public ContextAttributeEntry(String key) {
				this.key = key;
			}

			@Override
			public String getKey() {
				return key;
			}

			@Override
			public Object getValue() {
				return contextHandler.getAttribute(key);
			}

			@Override
			public Object setValue(Object value) {
				Object previousValue = getValue();
				contextHandler.setAttribute(key, value);
				return previousValue;
			}

		}
	}
}
