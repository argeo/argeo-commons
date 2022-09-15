package org.argeo.util.dav;

import java.io.IOException;

import org.argeo.util.http.HttpMethod;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class DavServerHandler implements HttpHandler {

	@Override
	public void handle(HttpExchange exchange) throws IOException {
		String method = exchange.getRequestMethod();
		if (DavMethod.PROPFIND.name().equals(method)) {
			handlePROPFIND(exchange);
		} else if (HttpMethod.GET.name().equals(method)) {
			handleGET(exchange);
		} else {
			throw new IllegalArgumentException("Unsupported method " + method);
		}

	}

	protected void handleGET(HttpExchange exchange) {
		throw new UnsupportedOperationException();
	}

	protected DavResponse handlePROPFIND(HttpExchange exchange) {
		throw new UnsupportedOperationException();
	}

}
