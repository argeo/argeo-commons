package org.argeo.cms.jetty;

import java.util.Map;

import javax.servlet.ServletContext;
import javax.websocket.DeploymentException;
import javax.websocket.server.ServerContainer;

import org.argeo.cms.servlet.httpserver.HttpContextServlet;
import org.argeo.cms.websocket.server.WebsocketEndpoints;
import org.eclipse.jetty.ee8.nested.SessionHandler;
import org.eclipse.jetty.ee8.servlet.ServletContextHandler;
import org.eclipse.jetty.ee8.servlet.ServletHolder;
import org.eclipse.jetty.ee8.websocket.javax.server.config.JavaxWebSocketServletContainerInitializer;
import org.eclipse.jetty.ee8.websocket.javax.server.config.JavaxWebSocketServletContainerInitializer.Configurator;

import com.sun.net.httpserver.HttpHandler;

/**
 * An @{HttpContext} implementation based on a Jetty
 * {@link ServletContextHandler}.
 */
class ContextHandlerHttpContext extends JettyHttpContext {
	private final ServletContextHandler servletContextHandler;
	private final ContextHandlerAttributes attributes;

	public ContextHandlerHttpContext(JettyHttpServer httpServer, String path) {
		super(httpServer, path);

		// Jetty context handler
		this.servletContextHandler = new ServletContextHandler();
		servletContextHandler.setContextPath(path);
		HttpContextServlet servlet = new HttpContextServlet(this);
		servletContextHandler.addServlet(new ServletHolder(servlet), "/*");
		SessionHandler sessionHandler = new SessionHandler();
		// FIXME find a better default
		sessionHandler.setMaxInactiveInterval(-1);
		servletContextHandler.setSessionHandler(sessionHandler);

		attributes = new ContextHandlerAttributes(servletContextHandler);
	}

	@Override
	public void setHandler(HttpHandler handler) {
		super.setHandler(handler);

		// web socket
		if (handler instanceof WebsocketEndpoints) {
			JavaxWebSocketServletContainerInitializer.configure(servletContextHandler, new Configurator() {

				@Override
				public void accept(ServletContext servletContext, ServerContainer serverContainer)
						throws DeploymentException {
					for (Class<?> clss : ((WebsocketEndpoints) handler).getEndPoints()) {
						serverContainer.addEndpoint(clss);
					}
				}
			});
		}

		if (getJettyHttpServer().isStarted())
			try {
				servletContextHandler.start();
			} catch (Exception e) {
				throw new IllegalStateException("Cannot start context handler", e);
			}
	}

	@Override
	public Map<String, Object> getAttributes() {
		return attributes;
	}

	@Override
	protected ServletContextHandler getServletContextHandler() {
		return servletContextHandler;
	}

}
