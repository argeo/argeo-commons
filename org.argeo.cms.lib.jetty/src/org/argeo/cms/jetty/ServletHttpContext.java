package org.argeo.cms.jetty;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.websocket.DeploymentException;
import javax.websocket.server.ServerContainer;

import org.argeo.api.cms.CmsLog;
import org.argeo.cms.servlet.httpserver.HttpContextServlet;
import org.argeo.cms.websocket.server.WebsocketEndpoints;
import org.eclipse.jetty.ee8.servlet.ServletContextHandler;
import org.eclipse.jetty.ee8.servlet.ServletHolder;

import com.sun.net.httpserver.HttpHandler;

/**
 * A {@link JettyHttpContext} based on registering a servlet to the root handler
 * of the {@link JettyHttpServer}, in order to integrate the sessions.
 */
public class ServletHttpContext extends JettyHttpContext {
	private final static CmsLog log = CmsLog.getLog(ServletHttpContext.class);

	private Map<String, Object> attributes = Collections.synchronizedMap(new HashMap<>());

	public ServletHttpContext(JettyHttpServer httpServer, String path) {
		super(httpServer, path);

		ServletContextHandler rootContextHandler = httpServer.getRootContextHandler();
		HttpContextServlet servlet = new HttpContextServlet(this);
		rootContextHandler.addServlet(new ServletHolder(servlet), path + "*");
	}

	@Override
	public void setHandler(HttpHandler handler) {
		super.setHandler(handler);

		// web socket
		if (handler instanceof WebsocketEndpoints) {
			ServerContainer serverContainer = getJettyHttpServer().getRootServerContainer();
			for (Class<?> clss : ((WebsocketEndpoints) handler).getEndPoints()) {
				try {
					serverContainer.addEndpoint(clss);
					log.debug(() -> "Added web socket " + clss + " to " + getPath());
				} catch (DeploymentException e) {
					log.error("Cannot deploy Web Socket " + clss, e);
				}
			}
		}
	}

	@Override
	public Map<String, Object> getAttributes() {
		return attributes;
	}

	@Override
	protected ServletContextHandler getServletContextHandler() {
		return getJettyHttpServer().getRootContextHandler();
	}

}
