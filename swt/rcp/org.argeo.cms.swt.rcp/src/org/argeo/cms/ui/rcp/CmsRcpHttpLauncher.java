package org.argeo.cms.ui.rcp;

import static java.lang.System.Logger.Level.DEBUG;

import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.argeo.api.cms.CmsApp;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

/** Publishes one {@link CmsRcpServlet} per {@link CmsApp}. */
public class CmsRcpHttpLauncher {
	private final static Logger logger = System.getLogger(CmsRcpHttpLauncher.class.getName());
	private CompletableFuture<HttpServer> httpServer = new CompletableFuture<>();

	private CmsRcpDisplayFactory cmsRcpDisplayFactory;
	
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
		final String contextName = properties.get(CmsApp.CONTEXT_NAME_PROPERTY);
		if (contextName != null) {
			httpServer.thenAcceptAsync((httpServer) -> {
				httpServer.createContext("/" + contextName, new HttpHandler() {

					@Override
					public void handle(HttpExchange exchange) throws IOException {
						String path = exchange.getRequestURI().getPath();
						String uiName = path != null ? path.substring(path.lastIndexOf('/') + 1) : "";
						cmsRcpDisplayFactory.openCmsApp(cmsApp, uiName, null);
						exchange.sendResponseHeaders(200, -1);
						logger.log(Level.DEBUG, "Opened RCP UI  " + uiName + " of  CMS App /" + contextName);
					}
				});
			}).exceptionally(e -> {
				logger.log(Level.ERROR, "Cannot register RCO app " + contextName, e);
				return null;
			});
			logger.log(Level.DEBUG, "Registered RCP CMS APP /" + contextName);
		}
	}

	public void removeCmsApp(CmsApp cmsApp, Map<String, String> properties) {
		String contextName = properties.get(CmsApp.CONTEXT_NAME_PROPERTY);
		if (contextName != null) {
			httpServer.thenAcceptAsync((httpServer) -> {
				httpServer.removeContext("/" + contextName);
			});
		}
	}

	public void setHttpServer(HttpServer httpServer) {
		Integer httpPort = httpServer.getAddress().getPort();
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
		logger.log(DEBUG, "RCP available under " + baseUrl + ", written to " + runFile);
		this.httpServer.complete(httpServer);
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
