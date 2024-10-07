package org.argeo.cms.http.server;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.argeo.cms.http.HttpHeader.CONTENT_TYPE;
import static org.argeo.cms.http.HttpHeader.DATE;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.argeo.api.acr.ContentRepository;
import org.argeo.api.acr.ContentSession;
import org.argeo.cms.auth.RemoteAuthUtils;
import org.argeo.cms.http.CommonMediaType;
import org.argeo.cms.http.HttpHeader;
import org.argeo.cms.http.HttpMethod;
import org.argeo.cms.http.HttpStatus;

import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;

/** HTTP utilities on the server-side. */
public class HttpServerUtils {
	private final static String SLASH = "/";

	private static String extractPathWithingContext(HttpContext httpContext, String fullPath, boolean startWithSlash) {
		Objects.requireNonNull(fullPath);
		String contextPath = httpContext.getPath();
		if (!fullPath.startsWith(contextPath))
			throw new IllegalArgumentException(fullPath + " does not belong to context " + contextPath);
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

	/** Returns content session consistent with this HTTP context. */
	// TODO move this method to another, ACR-specific, class
	public static ContentSession getContentSession(ContentRepository contentRepository, HttpExchange exchange) {
		ContentSession session = RemoteAuthUtils.doAs(() -> contentRepository.get(),
				new HttpRemoteAuthExchange(exchange));
		return session;
	}

	/*
	 * QUERY PARAMETERS
	 */
	public static Optional<String> getParameter(Map<String, List<String>> parameters, String key) {
		if (!parameters.containsKey(key))
			return Optional.empty();
		List<String> values = parameters.get(key);
		if (values == null || values.size() != 1)
			throw new IllegalArgumentException("Parameter " + key + " is present but either, empty, or multiple");
		return Optional.of(values.get(0));
	}

	/** Returns the HTTP parameters form an {@link HttpExchange}. */
	public static Map<String, List<String>> parseParameters(HttpExchange exchange) {
		// TODO check encoding?
		Charset encoding = StandardCharsets.UTF_8;

		Map<String, List<String>> parameters = new HashMap<>();
		URI requestedUri = exchange.getRequestURI();
		String query = requestedUri.getRawQuery();
		parseQuery(query, parameters, encoding);

		// TODO do we really want to support POST?
		if (HttpMethod.POST.name().equalsIgnoreCase(exchange.getRequestMethod())) {
			String postQuery;
			try {
				// We do not close the stream on purpose, since the body still needs to be read
				BufferedReader br = new BufferedReader(new InputStreamReader(exchange.getRequestBody(), encoding));
				postQuery = br.readLine();
			} catch (IOException e) {
				throw new UncheckedIOException("Cannot read exchange body", e);
			}
			parseQuery(postQuery, parameters, encoding);
		}
		return parameters;
	}

	private static void parseQuery(String query, Map<String, List<String>> parameters, Charset encoding) {
		if (query == null)
			return;
		String pairs[] = query.split("[&]");
		for (String pair : pairs) {
			String param[] = pair.split("[=]");

			String key = null;
			String value = null;
			if (param.length > 0) {
				key = URLDecoder.decode(param[0], encoding);
			}

			if (param.length > 1) {
				value = URLDecoder.decode(param[1], encoding);
			}

			if (!parameters.containsKey(key))
				parameters.put(key, new ArrayList<>());
			parameters.get(key).add(value);
		}
	}

	/*
	 * HEADER UTILITIES
	 */
	/**
	 * Set content type. For text-based format (text/* and application/json) set
	 * UTF-8 charset.
	 */
	public static void setContentType(HttpExchange exchange, CommonMediaType mediaType) {
		setContentType(exchange, mediaType.get(), mediaType.isTextBased() ? UTF_8 : null);
	}

	public static void setContentType(HttpExchange exchange, CommonMediaType mediaType, Charset charset) {
		setContentType(exchange, mediaType.get(), charset);
	}

	public static void setContentType(HttpExchange exchange, String mediaType, Charset charset) {
		exchange.getResponseHeaders().set(CONTENT_TYPE.get(),
				HttpHeader.formatContentType(mediaType, charset.name(), null));
	}

	/** Set date header to the current time. */
	public static void setDateHeader(HttpExchange exchange) {
		setDateHeader(exchange, System.currentTimeMillis());
	}

	public static void setDateHeader(HttpExchange exchange, long epoch) {
		exchange.getResponseHeaders().set(DATE.get(), Long.toString(epoch));
	}

	/*
	 * STATUS UTILITIES
	 */
	/**
	 * Send a status code (typically an error) without a respons body. It calls
	 * {@link HttpExchange#sendResponseHeaders(int, long)}, so nothing else can be
	 * set or written after calling this method.
	 */
	public static void sendStatusOnly(HttpExchange exchange, HttpStatus status) throws IOException {
		exchange.sendResponseHeaders(status.get(), -1);
	}

	/** singleton */
	private HttpServerUtils() {

	}
}
