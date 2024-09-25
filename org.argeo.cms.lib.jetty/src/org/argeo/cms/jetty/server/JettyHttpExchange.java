package org.argeo.cms.jetty.server;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.System.Logger;
import java.net.InetSocketAddress;
import java.net.URI;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLSession;

import org.argeo.cms.auth.RemoteAuthSession;
import org.argeo.cms.http.server.AbstractCmsHttpExchange;
import org.eclipse.jetty.http.HttpField;
import org.eclipse.jetty.io.Content;
import org.eclipse.jetty.io.EndPoint;
import org.eclipse.jetty.io.ssl.SslConnection;
import org.eclipse.jetty.io.ssl.SslConnection.SslEndPoint;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.Session;

import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpsExchange;

/** Integrates {@link HttpsExchange} in a servlet container. */
class JettyHttpExchange extends AbstractCmsHttpExchange {
	private final static Logger logger = System.getLogger(JettyHttpExchange.class.getName());

	private final Request jettyRequest;
	private final Response jettyResponse;

	// see
	// https://github.com/jetty/jetty.project/blob/jetty-12.0.x/documentation/jetty/modules/code/examples/src/main/java/org/eclipse/jetty/docs/programming/migration/ServletToHandlerDocs.java
	// for mapping between the Servlet and Jetty APIs.

	public JettyHttpExchange(HttpContext httpContext, Request jettyRequest, Response jettyResponse) {
		super(httpContext);
		this.jettyRequest = jettyRequest;
		this.jettyResponse = jettyResponse;

		// request headers
		for (HttpField headerField : jettyRequest.getHeaders()) {
			requestHeaders.put(headerField.getName(), headerField.getValueList());
		}
	}

	/*
	 * CMS specific
	 */

	@Override
	protected RemoteAuthSession getRemoteAuthSession() {
		Session jettySession = jettyRequest.getSession(true);
		return jettySession == null ? null : new JettyAuthSession(jettySession);
	}

	@Override
	public InputStream doGetRequestBody() {
		return Content.Source.asInputStream(jettyRequest);
	}

	@Override
	public OutputStream doGetResponseBody() {
		return Content.Sink.asOutputStream(jettyResponse);
	}

	/*
	 * HttpExchange implementation
	 */

	@Override
	public SSLSession getSSLSession() {
		SSLSession sslSession = null;
		EndPoint endPoint = jettyRequest.getConnectionMetaData().getConnection().getEndPoint();
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
	public URI getRequestURI() {
		return jettyRequest.getHttpURI().toURI();
	}

	@Override
	public String getRequestMethod() {
		return jettyRequest.getMethod();
	}

	@Override
	public void close() {

		try {
			Content.Source.asInputStream(jettyRequest).close();
		} catch (IOException e) {
			logger.log(System.Logger.Level.WARNING, "Cannot close stream of request " + jettyRequest, e);
		}
		try {
			Content.Sink.asOutputStream(jettyResponse).close();
		} catch (IOException e) {
			logger.log(System.Logger.Level.WARNING, "Cannot close stream of response " + jettyResponse, e);
		}

	}

	@Override
	public void sendResponseHeaders(int rCode, long responseLength) throws IOException {
		for (String headerName : responseHeaders.keySet()) {
			for (String headerValue : responseHeaders.get(headerName)) {
				jettyResponse.getHeaders().put(headerName, headerValue);
			}
		}
		// TODO deal with content length etc.
		jettyResponse.setStatus(rCode);
	}

	@Override
	public InetSocketAddress getRemoteAddress() {
		// TODO support non IP socket address? (e.g. UNIX sockets)
		return (InetSocketAddress) jettyRequest.getConnectionMetaData().getRemoteSocketAddress();
	}

	@Override
	public int getResponseCode() {
		return jettyResponse.getStatus();
	}

	@Override
	public InetSocketAddress getLocalAddress() {
		// TODO support non IP socket address? (e.g. UNIX sockets)
		return (InetSocketAddress) jettyRequest.getConnectionMetaData().getLocalSocketAddress();
	}

	@Override
	public String getProtocol() {
		return jettyRequest.getConnectionMetaData().getProtocol();
	}

	@Override
	public Object getAttribute(String name) {
		return jettyRequest.getAttribute(name);
	}

	@Override
	public void setAttribute(String name, Object value) {
		jettyRequest.setAttribute(name, value);
	}
}
