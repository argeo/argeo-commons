package org.argeo.cms.ui.rcp.servlet;

import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import javax.servlet.Servlet;

import org.argeo.api.cms.CmsApp;
import org.argeo.cms.ui.rcp.CmsRcpDisplayFactory;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.http.HttpService;

/** Publishes one {@link CmsRcpServlet} per {@link CmsApp}. */
public class CmsRcpServletFactory {
	private final static Logger logger = System.getLogger(CmsRcpServletFactory.class.getName());

	private BundleContext bundleContext = FrameworkUtil.getBundle(CmsRcpServletFactory.class).getBundleContext();

	private CompletableFuture<EventAdmin> eventAdmin = new CompletableFuture<>();

	private Map<String, ServiceRegistration<Servlet>> registrations = Collections.synchronizedMap(new HashMap<>());

	public void init() {

	}

	public void destroy() {
		Path runFile = CmsRcpDisplayFactory.getUrlRunFile();
		try {
			if (Files.exists(runFile)) {
				Files.delete(runFile);
			}
		} catch (IOException e) {
			logger.log(Level.ERROR, "Cannot delete " + runFile, e);
		}
	}

	public void addCmsApp(CmsApp cmsApp, Map<String, String> properties) {
		String contextName = properties.get(CmsApp.CONTEXT_NAME_PROPERTY);
		if (contextName != null) {
			eventAdmin.thenAccept((eventAdmin) -> {
				CmsRcpServlet servlet = new CmsRcpServlet(eventAdmin, cmsApp);
				Hashtable<String, String> serviceProperties = new Hashtable<>();
				serviceProperties.put("osgi.http.whiteboard.servlet.pattern", "/" + contextName + "/*");
				ServiceRegistration<Servlet> sr = bundleContext.registerService(Servlet.class, servlet,
						serviceProperties);
				registrations.put(contextName, sr);
			});
		}
	}

	public void removeCmsApp(CmsApp cmsApp, Map<String, String> properties) {
		String contextName = properties.get(CmsApp.CONTEXT_NAME_PROPERTY);
		if (contextName != null) {
			ServiceRegistration<Servlet> sr = registrations.get(contextName);
			sr.unregister();
		}
	}

	public void setEventAdmin(EventAdmin eventAdmin) {
		this.eventAdmin.complete(eventAdmin);
	}

	public void setHttpService(HttpService httpService, Map<String, Object> properties) {
		Integer httpPort = Integer.parseInt(properties.get("http.port").toString());
		String baseUrl = "http://localhost:" + httpPort + "/";
		Path runFile = CmsRcpDisplayFactory.getUrlRunFile();
		try {
			if (!Files.exists(runFile)) {
				Files.createDirectories(runFile.getParent());
				// TODO give read permission only to the owner
				Files.createFile(runFile);
			} else {
				URI uri = URI.create(Files.readString(runFile));
				if (!httpPort.equals(uri.getPort()))
					if (!isPortAvailable(uri.getPort())) {
						throw new IllegalStateException("Another CMS is running on " + runFile);
					} else {
						logger.log(Level.WARNING,
								"Run file " + runFile + " found but port of " + uri + " is available. Overwriting...");
					}
			}
			Files.writeString(runFile, baseUrl, StandardCharsets.UTF_8);
		} catch (IOException e) {
			throw new RuntimeException("Cannot write run file to " + runFile, e);
		}
		logger.log(Level.DEBUG, "RCP available under " + baseUrl + ", written to " + runFile);
	}

	protected boolean isPortAvailable(int port) {
		ServerSocket ss = null;
		DatagramSocket ds = null;
		try {
			ss = new ServerSocket(port);
			ss.setReuseAddress(true);
			ds = new DatagramSocket(port);
			ds.setReuseAddress(true);
			return true;
		} catch (IOException e) {
		} finally {
			if (ds != null) {
				ds.close();
			}

			if (ss != null) {
				try {
					ss.close();
				} catch (IOException e) {
					/* should not be thrown */
				}
			}
		}

		return false;
	}
}
