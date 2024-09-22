package org.argeo.cms.jetty.ee8;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

import javax.servlet.ServletContext;
import javax.websocket.DeploymentException;
import javax.websocket.server.ServerContainer;

import org.argeo.cms.jetty.JettyHttpServer;
import org.eclipse.jetty.ee8.nested.SessionHandler;
import org.eclipse.jetty.ee8.servlet.ServletContextHandler;
import org.eclipse.jetty.ee8.websocket.javax.server.config.JavaxWebSocketServletContainerInitializer;
import org.eclipse.jetty.ee8.websocket.javax.server.config.JavaxWebSocketServletContainerInitializer.Configurator;
import org.eclipse.jetty.server.Handler;

/** A {@link JettyHttpServer} which is compatible with Equinox servlets. */
public class CmsJavaxJettyServer extends JettyHttpServer {
	private static final String CONTEXT_TEMPDIR = "javax.servlet.context.tempdir";
	// Equinox compatibility
	private static final String INTERNAL_CONTEXT_CLASSLOADER = "org.eclipse.equinox.http.jetty.internal.ContextClassLoader";
	private Path tempDir;

	private CompletableFuture<ServerContainer> serverContainer = new CompletableFuture<>();

	// FIXME
	private ServletContextHandler rootContextHandler;

	protected void addServlets(ServletContextHandler servletContextHandler) {
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
	protected Handler createRootHandler() {
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

		JavaxWebSocketServletContainerInitializer.configure(servletContextHandler, new Configurator() {

			@Override
			public void accept(ServletContext servletContext, ServerContainer serverContainer)
					throws DeploymentException {
				CmsJavaxJettyServer.this.serverContainer.complete(serverContainer);
			}
		});

		rootContextHandler = servletContextHandler;
		return servletContextHandler.get();
	}

//	@Override
//	protected ServerContainer getRootServerContainer() {
//		return serverContainer.join();
//	}

	@Override
	protected void configureRootHandler(Handler servletContextHandler) {
		addServlets(rootContextHandler);
	}

	/*
	 * WEB SOCKET
	 */

}
