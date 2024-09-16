package org.argeo.cms.servlet.httpserver;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

import javax.net.ssl.SSLSession;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpPrincipal;
import com.sun.net.httpserver.HttpsExchange;

/** Integrates {@link HttpsExchange} in a servlet container. */
class ServletHttpExchange extends HttpsExchange {
	private final HttpContext httpContext;
	private final HttpServletRequest httpServletRequest;
	private final HttpServletResponse httpServletResponse;

	private final Headers requestHeaders;
	private final Headers responseHeaders;

	private InputStream filteredIn;
	private OutputStream filteredOut;

	private HttpPrincipal principal;

	public ServletHttpExchange(HttpContext httpContext, HttpServletRequest httpServletRequest,
			HttpServletResponse httpServletResponse) {
		this.httpContext = httpContext;
		this.httpServletRequest = httpServletRequest;
		this.httpServletResponse = httpServletResponse;

		// request headers
		requestHeaders = new Headers();
		for (Enumeration<String> headerNames = httpServletRequest.getHeaderNames(); headerNames.hasMoreElements();) {
			String headerName = headerNames.nextElement();
			List<String> values = new ArrayList<>();
			for (Enumeration<String> headerValues = httpServletRequest.getHeaders(headerName); headerValues
					.hasMoreElements();)
				values.add(headerValues.nextElement());
			requestHeaders.put(headerName, values);
		}

		responseHeaders = new Headers();
	}

	@Override
	public SSLSession getSSLSession() {
		Object obj = httpServletRequest.getAttribute("javax.net.ssl.session");
		if (obj == null || !(obj instanceof SSLSession))
			throw new IllegalStateException("SSL session not found");
		return (SSLSession) obj;
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
		// TODO properly deal with charset?
		Charset encoding = StandardCharsets.UTF_8;
		Map<String, String[]> parameters = httpServletRequest.getParameterMap();
		StringJoiner sb = new StringJoiner("&");
		for (String key : parameters.keySet()) {
			for (String value : parameters.get(key)) {
				String pair = URLEncoder.encode(key, encoding) + '=' + URLEncoder.encode(value, encoding);
				sb.add(pair);
			}
		}
		return URI.create(httpServletRequest.getRequestURI() + (sb.length() != 0 ? '?' + sb.toString() : ""));
	}

	@Override
	public String getRequestMethod() {
		return httpServletRequest.getMethod();
	}

	@Override
	public HttpContext getHttpContext() {
		return httpContext;
	}

	@Override
	public void close() {
		try {
			httpServletRequest.getInputStream().close();
		} catch (IOException e) {
			// TODO use proper logging
			e.printStackTrace();
		}
		try {
			httpServletResponse.getOutputStream().close();
		} catch (IOException e) {
			// TODO use proper logging
			e.printStackTrace();
		}

	}

	@Override
	public InputStream getRequestBody() {
		try {
			if (filteredIn != null)
				return filteredIn;
			else
				return httpServletRequest.getInputStream();
		} catch (IOException e) {
			throw new IllegalStateException("Cannot get request body", e);
		}
	}

	@Override
	public OutputStream getResponseBody() {
		try {
			if (filteredOut != null)
				return filteredOut;
			else
				return httpServletResponse.getOutputStream();
		} catch (IOException e) {
			throw new IllegalStateException("Cannot get response body", e);
		}
	}

	@Override
	public void sendResponseHeaders(int rCode, long responseLength) throws IOException {
		for (String headerName : responseHeaders.keySet()) {
			for (String headerValue : responseHeaders.get(headerName)) {
				httpServletResponse.addHeader(headerName, headerValue);
			}
		}
		// TODO deal with content length etc.
		httpServletResponse.setStatus(rCode);
	}

	@Override
	public InetSocketAddress getRemoteAddress() {
		return new InetSocketAddress(httpServletRequest.getRemoteHost(), httpServletRequest.getRemotePort());
	}

	@Override
	public int getResponseCode() {
		return httpServletResponse.getStatus();
	}

	@Override
	public InetSocketAddress getLocalAddress() {
		return new InetSocketAddress(httpServletRequest.getLocalName(), httpServletRequest.getLocalPort());
	}

	@Override
	public String getProtocol() {
		return httpServletRequest.getProtocol();
	}

	@Override
	public Object getAttribute(String name) {
		return httpServletRequest.getAttribute(name);
	}

	@Override
	public void setAttribute(String name, Object value) {
		httpServletRequest.setAttribute(name, value);
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
