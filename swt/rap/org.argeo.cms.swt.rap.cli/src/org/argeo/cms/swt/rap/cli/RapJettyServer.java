package org.argeo.cms.swt.rap.cli;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletException;

import org.argeo.cms.jetty.CmsJettyServer;
import org.argeo.cms.web.CmsWebApp;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.rap.rwt.application.ApplicationRunner;
import org.eclipse.rap.rwt.engine.RWTServlet;

public class RapJettyServer extends CmsJettyServer {
	private CmsWebApp cmsWebApp;

	@Override
	protected void addServlets(ServletContextHandler servletContextHandler) throws ServletException {
		// rwt-resources requires a file system
		try {
			Path tempDir = Files.createTempDirectory("argeo-rwtRunner");
			servletContextHandler.setBaseResource(Resource.newResource(tempDir.resolve("www").toString()));
		} catch (IOException e) {
			throw new IllegalStateException("Cannot create temporary directory", e);
		}
		servletContextHandler.addEventListener(new ServletContextListener() {
			ApplicationRunner applicationRunner;

			@Override
			public void contextInitialized(ServletContextEvent sce) {
				applicationRunner = new ApplicationRunner(cmsWebApp, sce.getServletContext());
				applicationRunner.start();
			}

			@Override
			public void contextDestroyed(ServletContextEvent sce) {
				applicationRunner.stop();
			}
		});
		for (String uiName : cmsWebApp.getCmsApp().getUiNames())
			servletContextHandler.addServlet(new ServletHolder(new RWTServlet()), "/" + uiName);

		// Required to serve rwt-resources. It is important that this is last.
		ServletHolder holderPwd = new ServletHolder("default", DefaultServlet.class);
		servletContextHandler.addServlet(holderPwd, "/");

	}

	public void setCmsWebApp(CmsWebApp cmsWebApp) {
		this.cmsWebApp = cmsWebApp;
	}

}
