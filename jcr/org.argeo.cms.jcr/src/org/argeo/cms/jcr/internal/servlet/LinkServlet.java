package org.argeo.cms.jcr.internal.servlet;

import static javax.jcr.Property.JCR_DESCRIPTION;
import static javax.jcr.Property.JCR_LAST_MODIFIED;
import static javax.jcr.Property.JCR_TITLE;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.PrivilegedExceptionAction;
import java.util.Calendar;
import java.util.Collection;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.security.auth.Subject;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.argeo.api.cms.CmsAuth;
import org.argeo.api.cms.CmsConstants;
import org.argeo.cms.CmsException;
import org.argeo.cms.jcr.CmsJcrUtils;
import org.argeo.jcr.JcrUtils;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;

public class LinkServlet extends HttpServlet {
	private final BundleContext bc = FrameworkUtil.getBundle(getClass()).getBundleContext();

	private static final long serialVersionUID = 3749990143146845708L;

	@Override
	protected void service(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		String path = request.getPathInfo();
		String userAgent = request.getHeader("User-Agent").toLowerCase();
		boolean isBot = false;
		// boolean isCompatibleBrowser = false;
		if (userAgent.contains("bot") || userAgent.contains("facebook") || userAgent.contains("twitter")) {
			isBot = true;
		}
		// else if (userAgent.contains("webkit") ||
		// userAgent.contains("gecko") || userAgent.contains("firefox")
		// || userAgent.contains("msie") || userAgent.contains("chrome") ||
		// userAgent.contains("chromium")
		// || userAgent.contains("opera") || userAgent.contains("browser"))
		// {
		// isCompatibleBrowser = true;
		// }

		if (isBot) {
			// log.warn("# BOT " + request.getHeader("User-Agent"));
			canonicalAnswer(request, response, path);
			return;
		}

		// if (isCompatibleBrowser && log.isTraceEnabled())
		// log.trace("# BWS " + request.getHeader("User-Agent"));
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
	private void canonicalAnswer(HttpServletRequest request, HttpServletResponse response, String path) {
		Session session = null;
		try {
			PrintWriter writer = response.getWriter();
			session = Subject.doAs(anonymousLogin(), new PrivilegedExceptionAction<Session>() {

				@Override
				public Session run() throws Exception {
					Collection<ServiceReference<Repository>> srs = bc.getServiceReferences(Repository.class,
							"(" + CmsConstants.CN + "=" + CmsConstants.EGO_REPOSITORY + ")");
					Repository repository = bc.getService(srs.iterator().next());
					return repository.login();
				}

			});
			Node node = session.getNode(path);
			String title = node.hasProperty(JCR_TITLE) ? node.getProperty(JCR_TITLE).getString() : node.getName();
			String desc = node.hasProperty(JCR_DESCRIPTION) ? node.getProperty(JCR_DESCRIPTION).getString() : null;
			Calendar lastUpdate = node.hasProperty(JCR_LAST_MODIFIED) ? node.getProperty(JCR_LAST_MODIFIED).getDate()
					: null;
			String url = getCanonicalUrl(node, request);
			String imgUrl = null;
			// TODO support images
//			loop: for (NodeIterator it = node.getNodes(); it.hasNext();) {
//				// Takes the first found cms:image
//				Node child = it.nextNode();
//				if (child.isNodeType(CMS_IMAGE)) {
//					imgUrl = getDataUrl(child, request);
//					break loop;
//				}
//			}
			StringBuilder buf = new StringBuilder();
			buf.append("<html>");
			buf.append("<head>");
			writeMeta(buf, "og:title", escapeHTML(title));
			writeMeta(buf, "og:type", "website");
			buf.append("<meta name='twitter:card' content='summary' />");
			buf.append("<meta name='twitter:site' content='@argeo_org' />");
			writeMeta(buf, "og:url", url);
			if (desc != null)
				writeMeta(buf, "og:description", escapeHTML(desc));
			if (imgUrl != null)
				writeMeta(buf, "og:image", imgUrl);
			if (lastUpdate != null)
				writeMeta(buf, "og:updated_time", Long.toString(lastUpdate.getTime().getTime()));
			buf.append("</head>");
			buf.append("<body>");
			buf.append("<p><b>!! This page is meant for indexing robots, not for real people," + " visit <a href='/#")
					.append(path).append("'>").append(escapeHTML(title)).append("</a> instead.</b></p>");
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

	/**
	 * From http://stackoverflow.com/questions/1265282/recommended-method-for-
	 * escaping-html-in-java (+ escaping '). TODO Use
	 * org.apache.commons.lang.StringEscapeUtils
	 */
	private String escapeHTML(String s) {
		StringBuilder out = new StringBuilder(Math.max(16, s.length()));
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			if (c > 127 || c == '\'' || c == '"' || c == '<' || c == '>' || c == '&') {
				out.append("&#");
				out.append((int) c);
				out.append(';');
			} else {
				out.append(c);
			}
		}
		return out.toString();
	}

	private void writeMeta(StringBuilder buf, String tag, String value) {
		buf.append("<meta property='").append(tag).append("' content='").append(value).append("'/>");
	}

	private void writeCanonical(StringBuilder buf, Node node) throws RepositoryException {
		buf.append("<div>");
		if (node.hasProperty(JCR_TITLE))
			buf.append("<p>").append(node.getProperty(JCR_TITLE).getString()).append("</p>");
		if (node.hasProperty(JCR_DESCRIPTION))
			buf.append("<p>").append(node.getProperty(JCR_DESCRIPTION).getString()).append("</p>");
		NodeIterator children = node.getNodes();
		while (children.hasNext()) {
			writeCanonical(buf, children.nextNode());
		}
		buf.append("</div>");
	}

	// DATA
	private StringBuilder getServerBaseUrl(HttpServletRequest request) {
		try {
			URL url = new URL(request.getRequestURL().toString());
			StringBuilder buf = new StringBuilder();
			buf.append(url.getProtocol()).append("://").append(url.getHost());
			if (url.getPort() != -1)
				buf.append(':').append(url.getPort());
			return buf;
		} catch (MalformedURLException e) {
			throw new CmsException("Cannot extract server base URL from " + request.getRequestURL(), e);
		}
	}

	private String getDataUrl(Node node, HttpServletRequest request) throws RepositoryException {
		try {
			StringBuilder buf = getServerBaseUrl(request);
			buf.append(CmsJcrUtils.getDataPath(CmsConstants.EGO_REPOSITORY, node));
			return new URL(buf.toString()).toString();
		} catch (MalformedURLException e) {
			throw new CmsException("Cannot build data URL for " + node, e);
		}
	}

	// public static String getDataPath(Node node) throws
	// RepositoryException {
	// assert node != null;
	// String userId = node.getSession().getUserID();
	//// if (log.isTraceEnabled())
	//// log.trace(userId + " : " + node.getPath());
	// StringBuilder buf = new StringBuilder();
	// boolean isAnonymous =
	// userId.equalsIgnoreCase(NodeConstants.ROLE_ANONYMOUS);
	// if (isAnonymous)
	// buf.append(WEBDAV_PUBLIC);
	// else
	// buf.append(WEBDAV_PRIVATE);
	// Session session = node.getSession();
	// Repository repository = session.getRepository();
	// String cn;
	// if (repository.isSingleValueDescriptor(NodeConstants.CN)) {
	// cn = repository.getDescriptor(NodeConstants.CN);
	// } else {
	//// log.warn("No cn defined in repository, using " +
	// NodeConstants.NODE);
	// cn = NodeConstants.NODE;
	// }
	// return
	// buf.append('/').append(cn).append('/').append(session.getWorkspace().getName()).append(node.getPath())
	// .toString();
	// }

	private String getCanonicalUrl(Node node, HttpServletRequest request) throws RepositoryException {
		try {
			StringBuilder buf = getServerBaseUrl(request);
			buf.append('/').append('!').append(node.getPath());
			return new URL(buf.toString()).toString();
		} catch (MalformedURLException e) {
			throw new CmsException("Cannot build data URL for " + node, e);
		}
		// return request.getRequestURL().append('!').append(node.getPath())
		// .toString();
	}

	private Subject anonymousLogin() {
		Subject subject = new Subject();
		LoginContext lc;
		try {
			lc = new LoginContext(CmsAuth.LOGIN_CONTEXT_ANONYMOUS, subject);
			lc.login();
			return subject;
		} catch (LoginException e) {
			throw new CmsException("Cannot login as anonymous", e);
		}
	}

}
