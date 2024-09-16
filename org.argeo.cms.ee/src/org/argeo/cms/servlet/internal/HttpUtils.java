package org.argeo.cms.servlet.internal;

import java.util.Enumeration;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.argeo.api.cms.CmsLog;

public class HttpUtils {
//	public final static String HEADER_AUTHORIZATION = "Authorization";
//	public final static String HEADER_WWW_AUTHENTICATE = "WWW-Authenticate";

	static boolean isBrowser(String userAgent) {
		return userAgent.contains("webkit") || userAgent.contains("gecko") || userAgent.contains("firefox")
				|| userAgent.contains("msie") || userAgent.contains("chrome") || userAgent.contains("chromium")
				|| userAgent.contains("opera") || userAgent.contains("browser");
	}

	public static void logResponseHeaders(CmsLog log, HttpServletResponse response) {
		if (!log.isDebugEnabled())
			return;
		for (String headerName : response.getHeaderNames()) {
			Object headerValue = response.getHeader(headerName);
			log.debug(headerName + ": " + headerValue);
		}
	}

	public static void logRequestHeaders(CmsLog log, HttpServletRequest request) {
		if (!log.isDebugEnabled())
			return;
		for (Enumeration<String> headerNames = request.getHeaderNames(); headerNames.hasMoreElements();) {
			String headerName = headerNames.nextElement();
			Object headerValue = request.getHeader(headerName);
			log.debug(headerName + ": " + headerValue);
		}
		log.debug(request.getRequestURI() + "\n");
	}

	public static void logRequest(CmsLog log, HttpServletRequest request) {
		log.debug("contextPath=" + request.getContextPath());
		log.debug("servletPath=" + request.getServletPath());
		log.debug("requestURI=" + request.getRequestURI());
		log.debug("queryString=" + request.getQueryString());
		StringBuilder buf = new StringBuilder();
		// headers
		Enumeration<String> en = request.getHeaderNames();
		while (en.hasMoreElements()) {
			String header = en.nextElement();
			Enumeration<String> values = request.getHeaders(header);
			while (values.hasMoreElements())
				buf.append("  " + header + ": " + values.nextElement());
			buf.append('\n');
		}

		// attributed
		Enumeration<String> an = request.getAttributeNames();
		while (an.hasMoreElements()) {
			String attr = an.nextElement();
			Object value = request.getAttribute(attr);
			buf.append("  " + attr + ": " + value);
			buf.append('\n');
		}
		log.debug("\n" + buf);
	}

	private HttpUtils() {

	}
}
