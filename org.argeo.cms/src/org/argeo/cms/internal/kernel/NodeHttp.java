package org.argeo.cms.internal.kernel;

import java.io.IOException;
import java.security.cert.X509Certificate;
import java.util.Enumeration;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.argeo.cms.CmsException;
import org.argeo.jcr.ArgeoJcrConstants;
import org.eclipse.equinox.http.servlet.ExtendedHttpService;

/**
 * Intercepts and enriches http access, mainly focusing on security and
 * transactionality.
 */
@Deprecated
class NodeHttp implements KernelConstants, ArgeoJcrConstants {
	private final static Log log = LogFactory.getLog(NodeHttp.class);

	// Filters
	private final RootFilter rootFilter;

	// private final DoSFilter dosFilter;
	// private final QoSFilter qosFilter;

	NodeHttp(ExtendedHttpService httpService) {
		rootFilter = new RootFilter();
		// dosFilter = new CustomDosFilter();
		// qosFilter = new QoSFilter();

		try {
			httpService.registerFilter("/", rootFilter, null, null);
		} catch (Exception e) {
			throw new CmsException("Cannot register filters", e);
		}
	}

	public void destroy() {
	}

	/** Intercepts all requests. Authenticates. */
	class RootFilter extends HttpFilter {

		@Override
		public void doFilter(HttpSession httpSession,
				HttpServletRequest request, HttpServletResponse response,
				FilterChain filterChain) throws IOException, ServletException {
			if (log.isTraceEnabled()) {
				log.trace(request.getRequestURL().append(
						request.getQueryString() != null ? "?"
								+ request.getQueryString() : ""));
				logRequest(request);
			}

			String servletPath = request.getServletPath();

			// client certificate
			X509Certificate clientCert = extractCertificate(request);
			if (clientCert != null) {
				// TODO authenticate
				// if (log.isDebugEnabled())
				// log.debug(clientCert.getSubjectX500Principal().getName());
			}

			// skip data
			if (servletPath.startsWith(PATH_DATA)) {
				filterChain.doFilter(request, response);
				return;
			}

			// skip /ui (workbench) for the time being
			if (servletPath.startsWith(PATH_WORKBENCH)) {
				filterChain.doFilter(request, response);
				return;
			}

			// redirect long RWT paths to anchor
			String path = request.getRequestURI().substring(
					servletPath.length());
			int pathLength = path.length();
			if (pathLength != 0 && (path.charAt(0) == '/')
					&& !servletPath.endsWith("rwt-resources")
					&& !path.startsWith(KernelConstants.PATH_WORKBENCH)
					&& path.lastIndexOf('/') != 0) {
				String newLocation = request.getServletPath() + "#" + path;
				response.setHeader("Location", newLocation);
				response.setStatus(HttpServletResponse.SC_FOUND);
				return;
			}

			// process normally
			filterChain.doFilter(request, response);
		}
	}

	private void logRequest(HttpServletRequest request) {
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

	private X509Certificate extractCertificate(HttpServletRequest req) {
		X509Certificate[] certs = (X509Certificate[]) req
				.getAttribute("javax.servlet.request.X509Certificate");
		if (null != certs && certs.length > 0) {
			return certs[0];
		}
		return null;
	}

	// class CustomDosFilter extends DoSFilter {
	// @Override
	// protected String extractUserId(ServletRequest request) {
	// HttpSession httpSession = ((HttpServletRequest) request)
	// .getSession();
	// if (isSessionAuthenticated(httpSession)) {
	// String userId = ((SecurityContext) httpSession
	// .getAttribute(SPRING_SECURITY_CONTEXT_KEY))
	// .getAuthentication().getName();
	// return userId;
	// }
	// return super.extractUserId(request);
	//
	// }
	// }
}
