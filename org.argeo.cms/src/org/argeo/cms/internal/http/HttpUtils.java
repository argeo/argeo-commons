package org.argeo.cms.internal.http;

import java.util.Enumeration;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;

class HttpUtils {
	final static String HEADER_AUTHORIZATION = "Authorization";
	final static String HEADER_WWW_AUTHENTICATE = "WWW-Authenticate";

	final static String DEFAULT_PROTECTED_HANDLERS = "/org/argeo/cms/internal/http/protectedHandlers.xml";
	final static String WEBDAV_CONFIG = "/org/argeo/cms/internal/http/webdav-config.xml";

	static boolean isBrowser(String userAgent) {
		return userAgent.contains("webkit") || userAgent.contains("gecko") || userAgent.contains("firefox")
				|| userAgent.contains("msie") || userAgent.contains("chrome") || userAgent.contains("chromium")
				|| userAgent.contains("opera") || userAgent.contains("browser");
	}

	static void logRequestHeaders(Log log, HttpServletRequest request) {
		if (!log.isDebugEnabled())
			return;
		for (Enumeration<String> headerNames = request.getHeaderNames(); headerNames.hasMoreElements();) {
			String headerName = headerNames.nextElement();
			Object headerValue = request.getHeader(headerName);
			log.debug(headerName + ": " + headerValue);
		}
		log.debug(request.getRequestURI() + "\n");
	}

	static void logRequest(Log log,HttpServletRequest request) {
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
