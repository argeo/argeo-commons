package org.argeo.cms.jetty;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.ee10.servlet.SessionHandler;
import org.eclipse.jetty.ee10.websocket.jakarta.server.config.JakartaWebSocketServletContainerInitializer.Configurator;
import org.eclipse.jetty.ee10.websocket.jakarta.server.config.JakartaWebSocketServletContainerInitializer;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.websocket.DeploymentException;
import jakarta.websocket.server.ServerContainer;

/** A {@link JettyHttpServer} which is compatible with Equinox servlets. */
public class CmsJettyServer extends JettyHttpServer {
	private static final String CONTEXT_TEMPDIR = "javax.servlet.context.tempdir";
	// Equinox compatibility
	private static final String INTERNAL_CONTEXT_CLASSLOADER = "org.eclipse.equinox.http.jetty.internal.ContextClassLoader";
	private Path tempDir;

	private CompletableFuture<ServerContainer> serverContainer = new CompletableFuture<>();

	protected void addServlets(ServletContextHandler servletContextHandler) throws ServletException {
	}

	@Override
	public void start() {
		try {
			tempDir = Files.createTempDirectory("jetty");
		} catch (IOException e) {
			throw new IllegalStateException("Cannot create temp dir", e);
		}
		super.start();
	}

	@Override
	protected ServletContextHandler createRootContextHandler() {
		ServletContextHandler servletContextHandler = new ServletContextHandler();
		servletContextHandler.setAttribute(INTERNAL_CONTEXT_CLASSLOADER,
				Thread.currentThread().getContextClassLoader());
		servletContextHandler.setClassLoader(this.getClass().getClassLoader());
		servletContextHandler.setContextPath("/");

		servletContextHandler.setAttribute(CONTEXT_TEMPDIR, tempDir.toAbsolutePath().toFile());
		SessionHandler handler = new SessionHandler();
		// FIXME deal with long running session
		// handler.setMaxInactiveInterval(-1);
		servletContextHandler.setSessionHandler(handler);

		JakartaWebSocketServletContainerInitializer.configure(servletContextHandler, new Configurator() {

			@Override
			public void accept(ServletContext servletContext, ServerContainer serverContainer)
					throws DeploymentException {
				CmsJettyServer.this.serverContainer.complete(serverContainer);
			}
		});

		return servletContextHandler;
	}

	@Override
	protected ServerContainer getRootServerContainer() {
		return serverContainer.join();
	}

	@Override
	protected void configureRootContextHandler(ServletContextHandler servletContextHandler) throws ServletException {
		addServlets(servletContextHandler);
	}

	/*
	 * WEB SOCKET
	 */

}
