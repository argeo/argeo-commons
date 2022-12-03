package org.argeo.cms.http;

import java.net.URI;
import java.util.Objects;

import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;

/** HTTP utilities on the server-side. */
public class HttpServerUtils {
	private final static String SLASH = "/";

	private static String extractPathWithingContext(HttpContext httpContext, String fullPath, boolean startWithSlash) {
		Objects.requireNonNull(fullPath);
		String contextPath = httpContext.getPath();
		if (!fullPath.startsWith(contextPath))
			throw new IllegalArgumentException(fullPath + " does not belong to context" + contextPath);
		String path = fullPath.substring(contextPath.length());
		// TODO optimise?
		if (!startWithSlash && path.startsWith(SLASH)) {
			path = path.substring(1);
		} else if (startWithSlash && !path.startsWith(SLASH)) {
			path = SLASH + path;
		}
		return path;
	}

	/** Path within the context, NOT starting with a slash. */
	public static String relativize(HttpExchange exchange) {
		URI uri = exchange.getRequestURI();
		HttpContext httpContext = exchange.getHttpContext();
		return extractPathWithingContext(httpContext, uri.getPath(), false);
	}

	/** Path within the context, starting with a slash. */
	public static String subPath(HttpExchange exchange) {
		URI uri = exchange.getRequestURI();
		HttpContext httpContext = exchange.getHttpContext();
		return extractPathWithingContext(httpContext, uri.getPath(), true);
	}

	/** singleton */
	private HttpServerUtils() {

	}
}
