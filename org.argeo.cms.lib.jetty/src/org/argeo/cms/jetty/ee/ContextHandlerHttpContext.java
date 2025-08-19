package org.argeo.cms.jetty.ee;

import java.util.Map;

import org.argeo.cms.jakarta.servlet.httpserver.HttpContextServlet;
import org.argeo.cms.jakarta.websocket.server.WebsocketEndpoints;
import org.argeo.cms.jetty.AbstractJettyHttpContext;
import org.argeo.cms.jetty.ContextHandlerAttributes;
import org.argeo.cms.jetty.JettyHttpServer;
import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.ee10.servlet.ServletHolder;
import org.eclipse.jetty.ee10.servlet.SessionHandler;
import org.eclipse.jetty.ee10.websocket.jakarta.server.config.JakartaWebSocketServletContainerInitializer;
import org.eclipse.jetty.ee10.websocket.jakarta.server.config.JakartaWebSocketServletContainerInitializer.Configurator;

import com.sun.net.httpserver.HttpHandler;

import jakarta.servlet.ServletContext;
import jakarta.websocket.DeploymentException;
import jakarta.websocket.server.ServerContainer;

/**
 * An @{HttpContext} implementation based on a Jetty
 * {@link ServletContextHandler}.
 */
public class ContextHandlerHttpContext extends AbstractJettyHttpContext {
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
		// FIXME find out how to have long-running sessions
		// sessionHandler.setMaxInactiveInterval(-1);
		servletContextHandler.setSessionHandler(sessionHandler);

		attributes = new ContextHandlerAttributes(servletContextHandler);
	}

	@Override
	public void setHandler(HttpHandler handler) {
		super.setHandler(handler);

		// web socket
		if (handler instanceof WebsocketEndpoints) {
			JakartaWebSocketServletContainerInitializer.configure(servletContextHandler, new Configurator() {

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
	protected ServletContextHandler getJettyHandler() {
		return servletContextHandler;
	}

}
