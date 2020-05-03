package org.argeo.eclipse.ui.jetty;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.rap.rwt.application.AbstractEntryPoint;
import org.eclipse.rap.rwt.application.ApplicationRunner;
import org.eclipse.rap.rwt.engine.RWTServlet;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;

/** A minimal RWT runner based on embedded Jetty. */
public class RwtRunner {

	private final Server server;
	private final ServerConnector serverConnector;
	private Path tempDir;

	public RwtRunner() {
		server = new Server(new QueuedThreadPool(10, 1));
		serverConnector = new ServerConnector(server);
		serverConnector.setPort(0);
		server.setConnectors(new Connector[] { serverConnector });
	}

	protected Control createUi(Composite parent, Object context) {
		return new Label(parent, SWT.NONE);
	}

	public void init() {
		ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
		context.setContextPath("/");
		server.setHandler(context);

		String entryPoint = "app";

		// rwt-resources requires a file system
		try {
			tempDir = Files.createTempDirectory("argeo-rwtRunner");
			context.setBaseResource(Resource.newResource(tempDir.resolve("www").toString()));
		} catch (IOException e) {
			throw new IllegalStateException("Cannot create temporary directory", e);
		}
		context.addEventListener(new ServletContextListener() {
			ApplicationRunner applicationRunner;

			@Override
			public void contextInitialized(ServletContextEvent sce) {
				applicationRunner = new ApplicationRunner(
						(application) -> application.addEntryPoint("/" + entryPoint, () -> new AbstractEntryPoint() {
							private static final long serialVersionUID = 5678385921969090733L;

							@Override
							protected void createContents(Composite parent) {
								createUi(parent, null);
							}
						}, null), sce.getServletContext());
				applicationRunner.start();
			}

			@Override
			public void contextDestroyed(ServletContextEvent sce) {
				applicationRunner.stop();
			}
		});

		context.addServlet(new ServletHolder(new RWTServlet()), "/" + entryPoint);

		// Required to serve rwt-resources. It is important that this is last.
		ServletHolder holderPwd = new ServletHolder("default", DefaultServlet.class);
		context.addServlet(holderPwd, "/");

		try {
			server.start();
		} catch (Exception e) {
			throw new IllegalStateException("Cannot start Jetty server", e);
		}
	}

	public void destroy() {
		try {
			serverConnector.close();
			server.stop();
			// TODO delete temp dir
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public Integer getEffectivePort() {
		return serverConnector.getLocalPort();
	}

	public void waitFor() throws InterruptedException {
		server.join();
	}

	public static void main(String[] args) throws Exception {
		RwtRunner rwtRunner = new RwtRunner() {

			@Override
			protected Control createUi(Composite parent, Object context) {
				Label label = new Label(parent, SWT.NONE);
				label.setText("Hello world!");
				return label;
			}
		};
		rwtRunner.init();
		Runtime.getRuntime().addShutdownHook(new Thread(() -> rwtRunner.destroy(), "Jetty shutdown"));

		long jvmUptime = ManagementFactory.getRuntimeMXBean().getUptime();
		System.out.println("App available in " + jvmUptime + " ms, on port " + rwtRunner.getEffectivePort());

		// open browser in app mode
		Thread.sleep(2000);// wait for RWT to be ready
		Runtime.getRuntime().exec("google-chrome --app=http://localhost:" + rwtRunner.getEffectivePort() + "/app");

		rwtRunner.waitFor();
	}
}
