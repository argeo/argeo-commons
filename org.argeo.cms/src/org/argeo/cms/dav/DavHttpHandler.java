package org.argeo.cms.dav;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.StringJoiner;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import javax.xml.namespace.NamespaceContext;

import org.argeo.api.acr.ContentNotFoundException;
import org.argeo.api.cms.CmsLog;
import org.argeo.cms.http.HttpHeader;
import org.argeo.cms.http.HttpMethod;
import org.argeo.cms.http.HttpStatus;
import org.argeo.cms.http.server.HttpServerUtils;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

/**
 * Centralise patterns which are not ACR specific. Not really meant as a
 * framework for building WebDav servers, but rather to make upper-level of
 * ACR-specific code more readable and maintainable.
 */
public abstract class DavHttpHandler implements HttpHandler {
	private final static CmsLog log = CmsLog.getLog(DavHttpHandler.class);

	@Override
	public void handle(HttpExchange exchange) throws IOException {
		String subPath = HttpServerUtils.subPath(exchange);
		String method = exchange.getRequestMethod();
		try {
			if (HttpMethod.GET.name().equals(method)) {
				handleGET(exchange, subPath);
			} else if (HttpMethod.OPTIONS.name().equals(method)) {
				handleOPTIONS(exchange, subPath);
				exchange.sendResponseHeaders(HttpStatus.NO_CONTENT.getCode(), -1);
			} else if (HttpMethod.PROPFIND.name().equals(method)) {
				DavDepth depth = DavDepth.fromHttpExchange(exchange);
				if (depth == null) {
					// default, as per http://www.webdav.org/specs/rfc4918.html#METHOD_PROPFIND
					depth = DavDepth.DEPTH_INFINITY;
				}
				DavPropfind davPropfind;
				try (InputStream in = exchange.getRequestBody()) {
					davPropfind = DavPropfind.load(depth, in);
				}
				MultiStatusWriter multiStatusWriter = new MultiStatusWriter(exchange.getProtocol());
				CompletableFuture<Void> published = handlePROPFIND(exchange, subPath, davPropfind, multiStatusWriter);
				exchange.sendResponseHeaders(HttpStatus.MULTI_STATUS.getCode(), 0l);
				NamespaceContext namespaceContext = getNamespaceContext(exchange, subPath);
				try (OutputStream out = exchange.getResponseBody()) {
					multiStatusWriter.process(namespaceContext, out, published.minimalCompletionStage(),
							davPropfind.isPropname());
				}
			} else {
				throw new IllegalArgumentException("Unsupported method " + method);
			}
		} catch (ContentNotFoundException e) {
			exchange.sendResponseHeaders(HttpStatus.NOT_FOUND.getCode(), -1);
		}
		// TODO return a structured error message
		// TODO better filter application errors and failed login etc.
		catch (UnsupportedOperationException e) {
			e.printStackTrace();
			exchange.sendResponseHeaders(HttpStatus.NOT_IMPLEMENTED.getCode(), -1);
		} catch (Exception e) {
			log.error("Failed HTTP exchange " + exchange.getRequestURI(), e);
			exchange.sendResponseHeaders(HttpStatus.INTERNAL_SERVER_ERROR.getCode(), -1);
		}

	}

	protected abstract NamespaceContext getNamespaceContext(HttpExchange httpExchange, String path);

	protected abstract CompletableFuture<Void> handlePROPFIND(HttpExchange exchange, String path,
			DavPropfind davPropfind, Consumer<DavResponse> consumer) throws IOException;

	protected abstract void handleGET(HttpExchange exchange, String path) throws IOException;

	protected void handleOPTIONS(HttpExchange exchange, String path) throws IOException {
		exchange.getResponseHeaders().set(HttpHeader.DAV.getHeaderName(), "1, 3");
		StringJoiner methods = new StringJoiner(",");
		methods.add(HttpMethod.OPTIONS.name());
		methods.add(HttpMethod.HEAD.name());
		methods.add(HttpMethod.GET.name());
		methods.add(HttpMethod.POST.name());
		methods.add(HttpMethod.PUT.name());
		methods.add(HttpMethod.PROPFIND.name());
		// TODO :
		methods.add(HttpMethod.PROPPATCH.name());
		methods.add(HttpMethod.MKCOL.name());
		methods.add(HttpMethod.DELETE.name());
		methods.add(HttpMethod.MOVE.name());
		methods.add(HttpMethod.COPY.name());

		exchange.getResponseHeaders().add(HttpHeader.ALLOW.getHeaderName(), methods.toString());
	}

}
