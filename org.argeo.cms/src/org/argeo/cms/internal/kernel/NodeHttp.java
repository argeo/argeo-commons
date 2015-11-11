package org.argeo.cms.internal.kernel;

import static javax.jcr.Property.JCR_DESCRIPTION;
import static javax.jcr.Property.JCR_LAST_MODIFIED;
import static javax.jcr.Property.JCR_TITLE;
import static org.argeo.cms.CmsTypes.CMS_IMAGE;

import java.io.IOException;
import java.io.PrintWriter;
import java.security.PrivilegedExceptionAction;
import java.security.cert.X509Certificate;
import java.util.Calendar;
import java.util.Enumeration;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.security.auth.Subject;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.argeo.cms.CmsException;
import org.argeo.cms.util.CmsUtils;
import org.argeo.jcr.ArgeoJcrConstants;
import org.argeo.jcr.JcrUtils;
import org.eclipse.equinox.http.servlet.ExtendedHttpService;

/**
 * Intercepts and enriches http access, mainly focusing on security and
 * transactionality.
 */
class NodeHttp implements KernelConstants, ArgeoJcrConstants {
	private final static Log log = LogFactory.getLog(NodeHttp.class);

	// Filters
	// private final RootFilter rootFilter;

	// private final DoSFilter dosFilter;
	// private final QoSFilter qosFilter;

	private Repository repository;

	NodeHttp(ExtendedHttpService httpService, NodeRepository node) {
		this.repository = node;
		// rootFilter = new RootFilter();
		// dosFilter = new CustomDosFilter();
		// qosFilter = new QoSFilter();

		try {
			httpService.registerServlet("/!", new LinkServlet(repository),
					null, null);
		} catch (Exception e) {
			throw new CmsException("Cannot register filters", e);
		}
	}

	public void destroy() {
	}

	static class LinkServlet extends HttpServlet {
		private static final long serialVersionUID = 3749990143146845708L;
		private final Repository repository;

		public LinkServlet(Repository repository) {
			this.repository = repository;
		}

		@Override
		protected void service(HttpServletRequest request,
				HttpServletResponse response) throws ServletException,
				IOException {
			String path = request.getPathInfo();
			String userAgent = request.getHeader("User-Agent").toLowerCase();
			boolean isBot = false;
			boolean isCompatibleBrowser = false;
			if (userAgent.contains("bot") || userAgent.contains("facebook")
					|| userAgent.contains("twitter")) {
				isBot = true;
			} else if (userAgent.contains("webkit")
					|| userAgent.contains("gecko")
					|| userAgent.contains("firefox")
					|| userAgent.contains("msie")
					|| userAgent.contains("chrome")
					|| userAgent.contains("chromium")
					|| userAgent.contains("opera")
					|| userAgent.contains("browser")) {
				isCompatibleBrowser = true;
			}

			if (isBot) {
				log.warn("# BOT " + request.getHeader("User-Agent"));
				canonicalAnswer(request, response, path);
				return;
			}

			if (isCompatibleBrowser && log.isTraceEnabled())
				log.trace("# BWS " + request.getHeader("User-Agent"));
			redirectTo(response, "/#" + path);
		}

		private void redirectTo(HttpServletResponse response, String location) {
			response.setHeader("Location", location);
			response.setStatus(HttpServletResponse.SC_FOUND);
		}

		// private boolean canonicalAnswerNeededBy(HttpServletRequest request) {
		// String userAgent = request.getHeader("User-Agent").toLowerCase();
		// return userAgent.startsWith("facebookexternalhit/");
		// }

		/** For bots which don't understand RWT. */
		private void canonicalAnswer(HttpServletRequest request,
				HttpServletResponse response, String path) {
			Session session = null;
			try {
				PrintWriter writer = response.getWriter();
				session = Subject.doAs(KernelUtils.anonymousLogin(),
						new PrivilegedExceptionAction<Session>() {

							@Override
							public Session run() throws Exception {
								return repository.login();
							}

						});
				Node node = session.getNode(path);
				String title = node.hasProperty(JCR_TITLE) ? node.getProperty(
						JCR_TITLE).getString() : node.getName();
				String desc = node.hasProperty(JCR_DESCRIPTION) ? node
						.getProperty(JCR_DESCRIPTION).getString() : null;
				Calendar lastUpdate = node.hasProperty(JCR_LAST_MODIFIED) ? node
						.getProperty(JCR_LAST_MODIFIED).getDate() : null;
				String url = CmsUtils.getCanonicalUrl(node, request);
				String imgUrl = null;
				for (NodeIterator it = node.getNodes(); it.hasNext();) {
					Node child = it.nextNode();
					if (child.isNodeType(CMS_IMAGE))
						imgUrl = CmsUtils.getDataUrl(child, request);
				}
				StringBuilder buf = new StringBuilder();
				buf.append("<html>");
				buf.append("<head>");
				writeMeta(buf, "og:title", title);
				writeMeta(buf, "og:type", "website");
				writeMeta(buf, "og:url", url);
				if (desc != null)
					writeMeta(buf, "og:description", desc);
				if (imgUrl != null)
					writeMeta(buf, "og:image", imgUrl);
				if (lastUpdate != null)
					writeMeta(buf, "og:updated_time",
							Long.toString(lastUpdate.getTime().getTime()));
				buf.append("</head>");
				buf.append("<body>");
				buf.append(
						"<p><b>!! This page is meant for indexing robots, not for real people,"
								+ " visit <a href='/#").append(path)
						.append("'>").append(title)
						.append("</a> instead.</b></p>");
				writeCanonical(buf, node);
				buf.append("</body>");
				buf.append("</html>");
				writer.print(buf.toString());

				response.setHeader("Content-Type", "text/html");
				writer.flush();
			} catch (Exception e) {
				throw new CmsException("Cannot write canonical answer", e);
			} finally {
				JcrUtils.logoutQuietly(session);
			}
		}

		private void writeMeta(StringBuilder buf, String tag, String value) {
			buf.append("<meta property='").append(tag).append("' content='")
					.append(value).append("'/>");
		}

		private void writeCanonical(StringBuilder buf, Node node)
				throws RepositoryException {
			buf.append("<div>");
			if (node.hasProperty(JCR_TITLE))
				buf.append("<p>")
						.append(node.getProperty(JCR_TITLE).getString())
						.append("</p>");
			if (node.hasProperty(JCR_DESCRIPTION))
				buf.append("<p>")
						.append(node.getProperty(JCR_DESCRIPTION).getString())
						.append("</p>");
			NodeIterator children = node.getNodes();
			while (children.hasNext()) {
				writeCanonical(buf, children.nextNode());
			}
			buf.append("</div>");
		}
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
