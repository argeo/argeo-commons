package org.argeo.cms.jetty.ee;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.argeo.cms.jakarta.servlet.httpserver.HttpContextServlet;
import org.argeo.cms.jetty.AbstractJettyHttpContext;
import org.argeo.cms.jetty.JettyHttpServer;
import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.ee10.servlet.ServletHolder;

import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpHandler;

/**
 * A {@link HttpContext} implementation based on registering a servlet to the
 * root handler of the {@link JettyHttpServer}, in order to integrate the
 * sessions.
 */
@Deprecated
public class ServletHttpContext extends AbstractJettyHttpContext {
//	private final static CmsLog log = CmsLog.getLog(ServletHttpContext.class);

	private Map<String, Object> attributes = Collections.synchronizedMap(new HashMap<>());

	public ServletHttpContext(JettyHttpServer httpServer, String path) {
		super(httpServer, path);

		ServletContextHandler rootContextHandler = (ServletContextHandler) httpServer.getRootHandler();
		HttpContextServlet servlet = new HttpContextServlet(this);
		rootContextHandler.addServlet(new ServletHolder(servlet), path + "*");
	}

	@Override
	public void setHandler(HttpHandler handler) {
		super.setHandler(handler);

//		// web socket
//		if (handler instanceof WebsocketEndpoints) {
//			ServerContainer serverContainer = getJettyHttpServer().getRootServerContainer();
//			for (Class<?> clss : ((WebsocketEndpoints) handler).getEndPoints()) {
//				try {
//					serverContainer.addEndpoint(clss);
//					log.debug(() -> "Added web socket " + clss + " to " + getPath());
//				} catch (DeploymentException e) {
//					log.error("Cannot deploy Web Socket " + clss, e);
//				}
//			}
//		}
	}

	@Override
	public Map<String, Object> getAttributes() {
		return attributes;
	}

	@Override
	protected ServletContextHandler getJettyHandler() {
		return (ServletContextHandler) getJettyHttpServer().getRootHandler();
	}

}
