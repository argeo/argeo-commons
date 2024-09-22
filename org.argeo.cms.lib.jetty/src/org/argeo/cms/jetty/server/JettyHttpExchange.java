package org.argeo.cms.jetty.server;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.System.Logger;
import java.net.InetSocketAddress;
import java.net.URI;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLSession;

import org.eclipse.jetty.io.Content;
import org.eclipse.jetty.io.EndPoint;
import org.eclipse.jetty.io.ssl.SslConnection;
import org.eclipse.jetty.io.ssl.SslConnection.SslEndPoint;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Fields;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpPrincipal;
import com.sun.net.httpserver.HttpsExchange;

/** Integrates {@link HttpsExchange} in a servlet container. */
class JettyHttpExchange extends HttpsExchange {
	private final static Logger logger = System.getLogger(JettyHttpExchange.class.getName());
	// see
	// https://github.com/jetty/jetty.project/blob/jetty-12.0.x/documentation/jetty/modules/code/examples/src/main/java/org/eclipse/jetty/docs/programming/migration/ServletToHandlerDocs.java
	// for mapping between the Servelt and Jetty APIs.

	private final HttpContext httpContext;
	private final Request request;
	private final Response response;

	private final Headers requestHeaders;
	private final Headers responseHeaders;

	private InputStream filteredIn;
	private OutputStream filteredOut;

	private HttpPrincipal principal;

	public JettyHttpExchange(HttpContext httpContext, Request jettyRequest, Response jettyResponse) {
		this.httpContext = httpContext;
		this.request = jettyRequest;
		this.response = jettyResponse;

		// request headers
		requestHeaders = new Headers();

		Fields allParameters = Request.extractQueryParameters(request);
		for (Fields.Field parameter : allParameters) {
			requestHeaders.put(parameter.getName(), parameter.getValues());
		}

		responseHeaders = new Headers();
	}

	@Override
	public SSLSession getSSLSession() {
		SSLSession sslSession = null;
		EndPoint endPoint = request.getConnectionMetaData().getConnection().getEndPoint();
		if (endPoint instanceof SslEndPoint sslEndPoint) {
			SslConnection sslConnection = sslEndPoint.getSslConnection();
			SSLEngine sslEngine = sslConnection.getSSLEngine();
			sslSession = sslEngine.getSession();
		}
		if (sslSession == null)
			throw new IllegalStateException("SSL session not found");
		return sslSession;
	}

	@Override
	public Headers getRequestHeaders() {
		return requestHeaders;
	}

	@Override
	public Headers getResponseHeaders() {
		return responseHeaders;
	}

	@Override
	public URI getRequestURI() {
		return request.getHttpURI().toURI();
	}

	@Override
	public String getRequestMethod() {
		return request.getMethod();
	}

	@Override
	public HttpContext getHttpContext() {
		return httpContext;
	}

	@Override
	public void close() {

		try {
			Content.Source.asInputStream(request).close();
		} catch (IOException e) {
			logger.log(System.Logger.Level.WARNING, "Cannot close stream of request " + request, e);
		}
		try {
			Content.Sink.asOutputStream(response).close();
		} catch (IOException e) {
			logger.log(System.Logger.Level.WARNING, "Cannot close stream of response " + response, e);
		}

	}

	@Override
	public InputStream getRequestBody() {
		if (filteredIn != null)
			return filteredIn;
		else
			return Content.Source.asInputStream(request);
	}

	@Override
	public OutputStream getResponseBody() {
		if (filteredOut != null)
			return filteredOut;
		else
			return Content.Sink.asOutputStream(response);
	}

	@Override
	public void sendResponseHeaders(int rCode, long responseLength) throws IOException {
		for (String headerName : responseHeaders.keySet()) {
			for (String headerValue : responseHeaders.get(headerName)) {
				response.getHeaders().put(headerName, headerValue);
			}
		}
		// TODO deal with content length etc.
		response.setStatus(rCode);
	}

	@Override
	public InetSocketAddress getRemoteAddress() {
		// TODO support non IP socket address? (e.g. UNIX sockets)
		return (InetSocketAddress) request.getConnectionMetaData().getRemoteSocketAddress();
	}

	@Override
	public int getResponseCode() {
		return response.getStatus();
	}

	@Override
	public InetSocketAddress getLocalAddress() {
		// TODO support non IP socket address? (e.g. UNIX sockets)
		return (InetSocketAddress) request.getConnectionMetaData().getLocalSocketAddress();
	}

	@Override
	public String getProtocol() {
		return request.getConnectionMetaData().getProtocol();
	}

	@Override
	public Object getAttribute(String name) {
		return request.getAttribute(name);
	}

	@Override
	public void setAttribute(String name, Object value) {
		request.setAttribute(name, value);
	}

	@Override
	public void setStreams(InputStream i, OutputStream o) {
		if (i != null)
			filteredIn = i;
		if (o != null)
			filteredOut = o;

	}

	@Override
	public HttpPrincipal getPrincipal() {
		return principal;
	}

	void setPrincipal(HttpPrincipal principal) {
		this.principal = principal;
	}

}
