package org.argeo.cms.jetty;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.websocket.DeploymentException;
import javax.websocket.server.ServerContainer;
import javax.websocket.server.ServerEndpointConfig;
import com.sun.net.httpserver.Authenticator;
import com.sun.net.httpserver.HttpContext;

import org.argeo.api.cms.CmsState;
import org.argeo.cms.CmsDeployProperty;
import org.argeo.cms.websocket.javax.server.CmsWebSocketConfigurator;
import org.argeo.cms.websocket.javax.server.TestEndpoint;
import org.eclipse.jetty.server.session.SessionHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.websocket.javax.server.config.JavaxWebSocketServletContainerInitializer;
import org.eclipse.jetty.websocket.javax.server.config.JavaxWebSocketServletContainerInitializer.Configurator;

public class CmsJettyServer extends JettyHttpServer {
	private static final String CONTEXT_TEMPDIR = "javax.servlet.context.tempdir";
	// Equinox compatibility
	private static final String INTERNAL_CONTEXT_CLASSLOADER = "org.eclipse.equinox.http.jetty.internal.ContextClassLoader";
//	private static final CmsLog log = CmsLog.getLog(CmsJettyServer.class);

//	private Server server;
//	private Path tempDir;
//
//	private ServerConnector httpConnector;
//	private ServerConnector httpsConnector;
	private Path tempDir;

	// WebSocket
	private ServerContainer wsServerContainer;
	private ServerEndpointConfig.Configurator wsEndpointConfigurator;

	private CmsState cmsState;

	private Authenticator defaultAuthenticator;

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

	protected ServletContextHandler createRootContextHandler() {
		ServletContextHandler servletContextHandler = new ServletContextHandler();
		servletContextHandler.setAttribute(INTERNAL_CONTEXT_CLASSLOADER,
				Thread.currentThread().getContextClassLoader());
		servletContextHandler.setClassLoader(this.getClass().getClassLoader());
		servletContextHandler.setContextPath("/");

		servletContextHandler.setAttribute(CONTEXT_TEMPDIR, tempDir.toAbsolutePath().toFile());
		SessionHandler handler = new SessionHandler();
		handler.setMaxInactiveInterval(-1);
		servletContextHandler.setSessionHandler(handler);

		return servletContextHandler;
	}

	@Override
	protected void configureRootContextHandler(ServletContextHandler servletContextHandler) throws ServletException {
		addServlets(servletContextHandler);
		enableWebSocket(servletContextHandler);

	}

	@Override
	public synchronized HttpContext createContext(String path) {
		HttpContext httpContext = super.createContext(path);
		httpContext.setAuthenticator(defaultAuthenticator);
		return httpContext;
	}

	protected void enableWebSocket(ServletContextHandler servletContextHandler) {
		String webSocketEnabled = getDeployProperty(CmsDeployProperty.WEBSOCKET_ENABLED);
		// web socket
		if (webSocketEnabled != null && webSocketEnabled.equals(Boolean.toString(true))) {
			JavaxWebSocketServletContainerInitializer.configure(servletContextHandler, new Configurator() {

				@Override
				public void accept(ServletContext servletContext, ServerContainer serverContainer)
						throws DeploymentException {
					wsServerContainer = serverContainer;

					wsEndpointConfigurator = new CmsWebSocketConfigurator();

					ServerEndpointConfig config = ServerEndpointConfig.Builder
							.create(TestEndpoint.class, "/ws/test/events/").configurator(wsEndpointConfigurator)
							.build();
					try {
						wsServerContainer.addEndpoint(config);
					} catch (DeploymentException e) {
						throw new IllegalStateException("Cannot initalise the WebSocket server runtime.", e);
					}
				}
			});
		}
	}

	protected String getDeployProperty(CmsDeployProperty deployProperty) {
		return cmsState.getDeployProperty(deployProperty.getProperty());
	}

	public void setCmsState(CmsState cmsState) {
		this.cmsState = cmsState;
	}

	public void setDefaultAuthenticator(Authenticator defaultAuthenticator) {
		this.defaultAuthenticator = defaultAuthenticator;
	}

}
