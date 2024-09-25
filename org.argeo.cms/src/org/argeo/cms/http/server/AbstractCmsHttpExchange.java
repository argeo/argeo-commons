package org.argeo.cms.http.server;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;

import org.argeo.cms.auth.RemoteAuthSession;
import org.argeo.cms.internal.http.CmsAuthenticator;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpPrincipal;
import com.sun.net.httpserver.HttpsExchange;

/**
 * Base class to integrate {@link HttpsExchange} in a third-party HTTP server.
 * {@link CmsAuthenticator} only supports {@link HttpExchange} implementations
 * based on it.
 */
public abstract class AbstractCmsHttpExchange extends HttpsExchange {
	protected final HttpContext httpContext;
	protected final Headers requestHeaders;
	protected final Headers responseHeaders;

	private InputStream filteredIn;
	private OutputStream filteredOut;

	private HttpPrincipal principal;

	protected AbstractCmsHttpExchange(HttpContext httpContext) {
		this.httpContext = httpContext;

		// request headers
		requestHeaders = new Headers();
		responseHeaders = new Headers();
	}

	/*
	 * API
	 */
	/** The actual request body to use if filter streams have not been set. */
	protected abstract InputStream doGetRequestBody() throws IOException;

	/** The actual response body to use if filter streams have not been set. */
	protected abstract OutputStream doGetResponseBody() throws IOException;

	/**
	 * Set the authenticated principal after authentication. It will be called by
	 * {@link CmsAuthenticatorFilter}, so it should be overridden with great care.
	 */
	protected void setPrincipal(HttpPrincipal principal) {
		this.principal = principal;
	}

	/**
	 * Get a {@link RemoteAuthSession} to be used for authentication purposes. This
	 * method should be overridden by implementation wrapping a proper session
	 * implementation (typically servlets).
	 * 
	 * @return a minimal session facade persisting across request, or
	 *         <code>null</code> if not available.
	 */
	protected RemoteAuthSession getRemoteAuthSession() {
		return (RemoteAuthSession) getAttribute(RemoteAuthSession.class.getName());
	}

	/** The input stream set by a filter. */
	protected InputStream getFilteredIn() {
		return filteredIn;
	}

	/** The output stream set by a filter. */
	protected OutputStream getFilteredOut() {
		return filteredOut;
	}

	/*
	 * HttpExchange partial implementation
	 */

	@Override
	public final Headers getRequestHeaders() {
		return requestHeaders;
	}

	@Override
	public final Headers getResponseHeaders() {
		return responseHeaders;
	}

	@Override
	public final HttpContext getHttpContext() {
		return httpContext;
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

	@Override
	public InputStream getRequestBody() {
		try {
			if (filteredIn != null)
				return filteredIn;
			else
				return doGetRequestBody();
		} catch (IOException e) {
			throw new UncheckedIOException("Cannot get request body", e);
		}
	}

	@Override
	public OutputStream getResponseBody() {
		try {
			if (filteredOut != null)
				return filteredOut;
			else
				return doGetResponseBody();
		} catch (IOException e) {
			throw new UncheckedIOException("Cannot get response body", e);
		}
	}

}
