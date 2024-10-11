package org.argeo.cms.jakarta.servlet;

import static org.argeo.cms.http.HttpHeader.VIA;
import static org.argeo.cms.http.HttpHeader.X_FORWARDED_HOST;

import java.net.URI;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

/** Servlet utilities. */
public class ServletUtils {

	/**
	 * The base URL for this query (without any path component (not even an ending
	 * '/'), taking into account reverse proxies.
	 */
	public static StringBuilder getRequestUrlBase(HttpServletRequest req) {
		return getRequestUrlBase(req, false);
	}

	/**
	 * The base URL for this query (without any path component (not even an ending
	 * '/'), taking into account reverse proxies.
	 * 
	 * @param forceReverseProxyHttps if a reverse proxy is detected and this is set
	 *                               to true, the https scheme will be used. This is
	 *                               to work around issued when the an https reverse
	 *                               proxy is talking to an http application.
	 */
	public static StringBuilder getRequestUrlBase(HttpServletRequest req, boolean forceReverseProxyHttps) {
		List<String> viaHosts = new ArrayList<>();
		for (Enumeration<String> it = req.getHeaders(VIA.getHeaderName()); it.hasMoreElements();) {
			String[] arr = it.nextElement().split(" ");
			viaHosts.add(arr[1]);
		}

		String outerHost = viaHosts.isEmpty() ? null : viaHosts.get(0);
		if (outerHost == null) {
			// Try non-standard header
			String forwardedHost = req.getHeader(X_FORWARDED_HOST.getHeaderName());
			if (forwardedHost != null) {
				String[] arr = forwardedHost.split(",");
				outerHost = arr[0];
			}
		}

		URI requestUrl = URI.create(req.getRequestURL().toString());

		boolean isReverseProxy = outerHost != null && !outerHost.equals(requestUrl.getHost());
		if (isReverseProxy) {
			String protocol;
			if (forceReverseProxyHttps)
				protocol = "https";
			else
				protocol = req.isSecure() ? "https" : "http";
			return new StringBuilder(protocol + "://" + outerHost);
		} else {
			return new StringBuilder(requestUrl.getScheme() + "://" + requestUrl.getHost()
					+ (requestUrl.getPort() > 0 ? ":" + requestUrl.getPort() : ""));
		}
	}

	/** singleton */
	private ServletUtils() {
	}
}
