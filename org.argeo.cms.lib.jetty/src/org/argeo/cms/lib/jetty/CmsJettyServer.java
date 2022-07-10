package org.argeo.cms.lib.jetty;

import java.nio.file.Path;

import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.thread.QueuedThreadPool;

public class CmsJettyServer {
	private Server server;
	private ServerConnector serverConnector;
	private Path tempDir;

	public void start() {
		server = new Server(new QueuedThreadPool(10, 1));
		serverConnector = new ServerConnector(server);
		serverConnector.setPort(0);
		server.setConnectors(new Connector[] { serverConnector });

		ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
		context.setContextPath("/");
		server.setHandler(context);

		//context.addServlet(new ServletHolder(new RWTServlet()), "/" + entryPoint);

		// Required to serve rwt-resources. It is important that this is last.
		ServletHolder holderPwd = new ServletHolder("default", DefaultServlet.class);
		context.addServlet(holderPwd, "/");

		try {
			server.start();
		} catch (Exception e) {
			throw new IllegalStateException("Cannot start Jetty server", e);
		}
		Runtime.getRuntime().addShutdownHook(new Thread(() -> stop(), "Jetty shutdown"));
	}

	public void stop() {
		try {
			serverConnector.close();
			server.stop();
			// TODO delete temp dir
		} catch (Exception e) {
			e.printStackTrace();
		}

	}
}
