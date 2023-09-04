package org.argeo.cms.http.server;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.net.FileNameMap;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

import org.argeo.cms.acr.ContentUtils;
import org.argeo.cms.http.HttpHeader;
import org.argeo.cms.http.HttpStatus;
import org.argeo.cms.util.StreamUtils;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

/** A simple {@link HttpHandler} which just serves or proxy resources. */
public class StaticHttpHandler implements HttpHandler {
	private final static Logger logger = System.getLogger(StaticHttpHandler.class.getName());

	private static FileNameMap fileNameMap = URLConnection.getFileNameMap();

	private NavigableMap<String, Object> binds = new TreeMap<>();

	@Override
	public void handle(HttpExchange exchange) throws IOException {
		try {
			String path = HttpServerUtils.subPath(exchange);
			Map.Entry<String, Object> bindEntry = findBind(path);
			boolean isRoot = "/".equals(bindEntry.getKey());

			String relPath = isRoot ? path.substring(bindEntry.getKey().length())
					: path.substring(bindEntry.getKey().length() + 1);
			process(bindEntry.getValue(), exchange, relPath);
		} catch (Exception e) {
			logger.log(Level.ERROR, exchange.getRequestURI().toString(), e);
		}
	}

	public void addBind(String path, Object bind) {
		if (binds.containsKey(path))
			throw new IllegalStateException("Path '" + path + "' is already bound");
		Object bindToUse = checkBindSupport(bind);
		binds.put(path, bindToUse);
	}

	protected Map.Entry<String, Object> findBind(String path) {
		Map.Entry<String, Object> entry = binds.floorEntry(path);
		if (entry == null)
			return null;
		String mountPath = entry.getKey();
		if (!path.startsWith(mountPath)) {
			// FIXME make it more robust and find when there is no content provider
			String[] parent = ContentUtils.getParentPath(path);
			return findBind(parent[0]);
		}
		return entry;
	}

	protected void process(Object bind, HttpExchange httpExchange, String relativePath) throws IOException {
		OutputStream out = null;

		try {
			String contentType = fileNameMap.getContentTypeFor(relativePath);
			if (contentType != null)
				httpExchange.getResponseHeaders().set(HttpHeader.CONTENT_TYPE.getHeaderName(), contentType);

			if (bind instanceof Path bindPath) {
				Path path = bindPath.resolve(relativePath);
				if (!Files.exists(path)) {
					httpExchange.sendResponseHeaders(HttpStatus.NOT_FOUND.getCode(), -1);
					return;
				}
				long size = Files.size(path);
				httpExchange.sendResponseHeaders(HttpStatus.OK.getCode(), size);
				out = httpExchange.getResponseBody();
				Files.copy(path, out);
			} else if (bind instanceof URL bindUrl) {
				URL url = new URL(bindUrl.toString() + relativePath);
				URLConnection urlConnection;
				try {
					urlConnection = url.openConnection();
					urlConnection.connect();
				} catch (IOException e) {
					httpExchange.sendResponseHeaders(HttpStatus.NOT_FOUND.getCode(), -1);
					return;
				}
				// TODO check other headers?
				// TODO use Proxy?
				String contentLengthStr = urlConnection.getHeaderField(HttpHeader.CONTENT_LENGTH.getHeaderName());
				httpExchange.sendResponseHeaders(HttpStatus.OK.getCode(),
						contentLengthStr != null ? Long.parseLong(contentLengthStr) : 0);
				try (InputStream in = urlConnection.getInputStream()) {
					out = httpExchange.getResponseBody();
					StreamUtils.copy(in, out);
				} finally {
				}
			}
			// make sure everything is flushed
			httpExchange.getResponseBody().flush();
		} catch (RuntimeException e) {
			try {
				httpExchange.sendResponseHeaders(HttpStatus.INTERNAL_SERVER_ERROR.getCode(), -1);
			} catch (IOException e1) {
				// silent
			}
			throw e;
		} finally {
			if (out != null) {
				try {
					out.close();
				} catch (IOException e) {
					throw e;
				}
			}
		}
	}

	/**
	 * Checks whether this bind type is supported. This can be overridden in order
	 * to ass new bind type.
	 * 
	 * @see #process(Object, HttpExchange, String) for overriding the actual
	 *      implementation.
	 * 
	 * @param bind the bind to check
	 * @return the bind object to actually use (an URI will have been converted to
	 *         URL)
	 * @throws UnsupportedOperationException if this bind type is not supported
	 */
	protected Object checkBindSupport(Object bind) throws UnsupportedOperationException {
		if (bind instanceof Path)
			return bind;
		if (bind instanceof URL)
			return bind;
		if (bind instanceof URI uri) {
			try {
				return uri.toURL();
			} catch (MalformedURLException e) {
				throw new UnsupportedOperationException("URI " + uri + " cannot be connverted to URL.", e);
			}
		}
		// TODO string as a path within the server?
		throw new UnsupportedOperationException("Bind " + bind + " type " + bind.getClass() + " is not supported.");
	}

	public static void main(String... args) {
		try {
			HttpServer httpServer = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 6060), 0);

			StaticHttpHandler staticHttpHandler = new StaticHttpHandler();
			staticHttpHandler.addBind("/", Paths.get("/home/mbaudier/dev/workspaces/test-node-js/test-static"));
			staticHttpHandler.addBind("/js",
					Paths.get("/home/mbaudier/dev/workspaces/test-node-js/test-static/node_modules"));

			httpServer.createContext("/", staticHttpHandler);
			httpServer.start();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}
}
