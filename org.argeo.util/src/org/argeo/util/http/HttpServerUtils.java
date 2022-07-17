package org.argeo.util.http;

import java.net.URI;
import java.util.Objects;

import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;

public class HttpServerUtils {

	public static String relativize(HttpContext httpContext, String path) {
		Objects.requireNonNull(path);
		if (!path.startsWith(httpContext.getPath()))
			throw new IllegalArgumentException(path + " does not belong to context" + httpContext.getPath());
		String relativePath = path.substring(httpContext.getPath().length());
		// TODO optimise?
		if (relativePath.startsWith("/"))
			relativePath = relativePath.substring(1);
		return relativePath;
	}

	public static String relativize(HttpExchange exchange) {
		URI uri = exchange.getRequestURI();
		HttpContext httpContext = exchange.getHttpContext();
		return relativize(httpContext, uri.getPath());
	}

	/** singleton */
	private HttpServerUtils() {

	}
}
