package org.argeo.cms.internal.kernel;

import static org.argeo.jackrabbit.servlet.WebdavServlet.INIT_PARAM_RESOURCE_CONFIG;

import java.io.IOException;
import java.security.cert.X509Certificate;
import java.util.Enumeration;
import java.util.Properties;
import java.util.StringTokenizer;

import javax.jcr.Repository;
import javax.servlet.FilterChain;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.argeo.cms.CmsException;
import org.argeo.jackrabbit.servlet.OpenInViewSessionProvider;
import org.argeo.jackrabbit.servlet.RemotingServlet;
import org.argeo.jackrabbit.servlet.WebdavServlet;
import org.argeo.jcr.ArgeoJcrConstants;
import org.argeo.security.NodeAuthenticationToken;
import org.eclipse.equinox.http.servlet.ExtendedHttpService;
import org.osgi.service.http.NamespaceException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Intercepts and enriches http access, mainly focusing on security and
 * transactionality.
 */
class NodeHttp implements KernelConstants, ArgeoJcrConstants {
	private final static Log log = LogFactory.getLog(NodeHttp.class);

	private final static String ATTR_AUTH = "auth";
	private final static String HEADER_AUTHORIZATION = "Authorization";
	private final static String HEADER_WWW_AUTHENTICATE = "WWW-Authenticate";

	private final AuthenticationManager authenticationManager;
	private final ExtendedHttpService httpService;

	// FIXME Make it more unique
	private String httpAuthRealm = "Argeo";

	// Filters
	private final RootFilter rootFilter;
	// private final DoSFilter dosFilter;
	// private final QoSFilter qosFilter;

	// WebDav / JCR remoting
	private OpenInViewSessionProvider sessionProvider;

	NodeHttp(ExtendedHttpService httpService, JackrabbitNode node,
			NodeSecurity authenticationManager) {
		// this.bundleContext = bundleContext;
		this.authenticationManager = authenticationManager;

		this.httpService = httpService;

		// Filters
		rootFilter = new RootFilter();
		// dosFilter = new CustomDosFilter();
		// qosFilter = new QoSFilter();

		// DAV
		sessionProvider = new OpenInViewSessionProvider();

		registerRepositoryServlets(ALIAS_NODE, node);
		try {
			httpService.registerFilter("/", rootFilter, null, null);
		} catch (Exception e) {
			throw new CmsException("Could not register root filter", e);
		}
	}

	public void destroy() {
		sessionProvider.destroy();
		unregisterRepositoryServlets(ALIAS_NODE);
	}

	void registerRepositoryServlets(String alias, Repository repository) {
		try {
			registerWebdavServlet(alias, repository, true);
			registerWebdavServlet(alias, repository, false);
			registerRemotingServlet(alias, repository, true);
			registerRemotingServlet(alias, repository, false);
		} catch (Exception e) {
			throw new CmsException(
					"Could not register servlets for repository " + alias, e);
		}
	}

	void unregisterRepositoryServlets(String alias) {
		// FIXME unregister servlets
	}

	void registerWebdavServlet(String alias, Repository repository,
			boolean anonymous) throws NamespaceException, ServletException {
		WebdavServlet webdavServlet = new WebdavServlet(repository,
				sessionProvider);
		String pathPrefix = anonymous ? WEBDAV_PUBLIC : WEBDAV_PRIVATE;
		String path = pathPrefix + "/" + alias;
		Properties ip = new Properties();
		ip.setProperty(INIT_PARAM_RESOURCE_CONFIG, WEBDAV_CONFIG);
		ip.setProperty(WebdavServlet.INIT_PARAM_RESOURCE_PATH_PREFIX, path);
		httpService.registerFilter(path, anonymous ? new AnonymousFilter()
				: new DavFilter(), null, null);
		// Cast to servlet because of a weird behaviour in Eclipse
		httpService.registerServlet(path, (Servlet) webdavServlet, ip, null);
	}

	void registerRemotingServlet(String alias, Repository repository,
			boolean anonymous) throws NamespaceException, ServletException {
		String pathPrefix = anonymous ? REMOTING_PUBLIC : REMOTING_PRIVATE;
		RemotingServlet remotingServlet = new RemotingServlet(repository,
				sessionProvider);
		String path = pathPrefix + "/" + alias;
		Properties ip = new Properties();
		ip.setProperty(RemotingServlet.INIT_PARAM_RESOURCE_PATH_PREFIX, path);

		// Looks like a bug in Jackrabbit remoting init
		ip.setProperty(RemotingServlet.INIT_PARAM_HOME,
				KernelUtils.getOsgiInstanceDir() + "/tmp/jackrabbit");
		ip.setProperty(RemotingServlet.INIT_PARAM_TMP_DIRECTORY, "remoting");
		// Cast to servlet because of a weird behaviour in Eclipse
		httpService.registerFilter(path, anonymous ? new AnonymousFilter()
				: new DavFilter(), null, null);
		httpService.registerServlet(path, (Servlet) remotingServlet, ip, null);
	}

	// private Boolean isSessionAuthenticated(HttpSession httpSession) {
	// SecurityContext contextFromSession = (SecurityContext) httpSession
	// .getAttribute(SPRING_SECURITY_CONTEXT_KEY);
	// return contextFromSession != null;
	// }

	private void requestBasicAuth(HttpSession httpSession,
			HttpServletResponse response) {
		response.setStatus(401);
		response.setHeader(HEADER_WWW_AUTHENTICATE, "basic realm=\""
				+ httpAuthRealm + "\"");
		httpSession.setAttribute(ATTR_AUTH, Boolean.TRUE);
	}

	private NodeAuthenticationToken basicAuth(String authHeader) {
		if (authHeader != null) {
			StringTokenizer st = new StringTokenizer(authHeader);
			if (st.hasMoreTokens()) {
				String basic = st.nextToken();
				if (basic.equalsIgnoreCase("Basic")) {
					try {
						String credentials = new String(Base64.decodeBase64(st
								.nextToken()), "UTF-8");
						// log.debug("Credentials: " + credentials);
						int p = credentials.indexOf(":");
						if (p != -1) {
							String login = credentials.substring(0, p).trim();
							String password = credentials.substring(p + 1)
									.trim();

							return new NodeAuthenticationToken(login,
									password.toCharArray());
						} else {
							throw new CmsException(
									"Invalid authentication token");
						}
					} catch (Exception e) {
						throw new CmsException(
								"Couldn't retrieve authentication", e);
					}
				}
			}
		}
		throw new CmsException("Couldn't retrieve authentication");
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
				// logRequest(request);
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
		log.debug(request.getContextPath());
		log.debug(request.getServletPath());
		log.debug(request.getRequestURI());
		log.debug(request.getQueryString());
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

	/** Intercepts all requests. Authenticates. */
	private class AnonymousFilter extends HttpFilter {
		@Override
		public void doFilter(HttpSession httpSession,
				HttpServletRequest request, HttpServletResponse response,
				FilterChain filterChain) throws IOException, ServletException {

			// Authenticate from session
			// if (isSessionAuthenticated(httpSession)) {
			// filterChain.doFilter(request, response);
			// return;
			// }

			KernelUtils.anonymousLogin(authenticationManager);
			filterChain.doFilter(request, response);
		}
	}

	/** Intercepts all requests. Authenticates. */
	private class DavFilter extends HttpFilter {

		@Override
		public void doFilter(HttpSession httpSession,
				HttpServletRequest request, HttpServletResponse response,
				FilterChain filterChain) throws IOException, ServletException {

			// Authenticate from session
			// if (isSessionAuthenticated(httpSession)) {
			// filterChain.doFilter(request, response);
			// return;
			// }

			// Process basic auth
			String basicAuth = request.getHeader(HEADER_AUTHORIZATION);
			if (basicAuth != null) {
				UsernamePasswordAuthenticationToken token = basicAuth(basicAuth);
				Authentication auth = authenticationManager.authenticate(token);
				SecurityContextHolder.getContext().setAuthentication(auth);
				// httpSession.setAttribute(SPRING_SECURITY_CONTEXT_KEY,
				// SecurityContextHolder.getContext());
				// httpSession.setAttribute(ATTR_AUTH, Boolean.FALSE);
				filterChain.doFilter(request, response);
				return;
			}

			requestBasicAuth(httpSession, response);
		}
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
