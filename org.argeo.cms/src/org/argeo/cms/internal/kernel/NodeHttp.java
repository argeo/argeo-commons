package org.argeo.cms.internal.kernel;

import static org.argeo.cms.auth.AuthConstants.ACCESS_CONTROL_CONTEXT;

import java.io.IOException;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.security.cert.X509Certificate;
import java.util.Enumeration;
import java.util.Properties;
import java.util.StringTokenizer;

import javax.jcr.Repository;
import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
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
import org.argeo.cms.auth.AuthConstants;
import org.argeo.jackrabbit.servlet.OpenInViewSessionProvider;
import org.argeo.jackrabbit.servlet.RemotingServlet;
import org.argeo.jackrabbit.servlet.WebdavServlet;
import org.argeo.jcr.ArgeoJcrConstants;
import org.eclipse.equinox.http.servlet.ExtendedHttpService;
import org.osgi.service.http.NamespaceException;

/**
 * Intercepts and enriches http access, mainly focusing on security and
 * transactionality.
 */
@Deprecated
class NodeHttp implements KernelConstants, ArgeoJcrConstants {
	private final static Log log = LogFactory.getLog(NodeHttp.class);

	private final static String ATTR_AUTH = "auth";
	private final static String HEADER_AUTHORIZATION = "Authorization";
	private final static String HEADER_WWW_AUTHENTICATE = "WWW-Authenticate";

	// private final AuthenticationManager authenticationManager;
	private final ExtendedHttpService httpService;

	// FIXME Make it more unique
	private String httpAuthRealm = "Argeo";

	// Filters
	private final RootFilter rootFilter;
	// private final DoSFilter dosFilter;
	// private final QoSFilter qosFilter;

	// WebDav / JCR remoting
	private OpenInViewSessionProvider sessionProvider;

	NodeHttp(ExtendedHttpService httpService, NodeRepository node) {
		// this.bundleContext = bundleContext;
		// this.authenticationManager = authenticationManager;

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
		ip.setProperty(WebdavServlet.INIT_PARAM_RESOURCE_CONFIG, WEBDAV_CONFIG);
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
		// in order to avoid annoying warning.
		ip.setProperty(RemotingServlet.INIT_PARAM_PROTECTED_HANDLERS_CONFIG,
				"");
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

	private CallbackHandler basicAuth(String authHeader) {
		if (authHeader != null) {
			StringTokenizer st = new StringTokenizer(authHeader);
			if (st.hasMoreTokens()) {
				String basic = st.nextToken();
				if (basic.equalsIgnoreCase("Basic")) {
					try {
						// TODO manipulate char[]
						String credentials = new String(Base64.decodeBase64(st
								.nextToken()), "UTF-8");
						// log.debug("Credentials: " + credentials);
						int p = credentials.indexOf(":");
						if (p != -1) {
							final String login = credentials.substring(0, p)
									.trim();
							final char[] password = credentials
									.substring(p + 1).trim().toCharArray();

							return new CallbackHandler() {
								public void handle(Callback[] callbacks) {
									for (Callback cb : callbacks) {
										if (cb instanceof NameCallback)
											((NameCallback) cb).setName(login);
										else if (cb instanceof PasswordCallback)
											((PasswordCallback) cb)
													.setPassword(password);
									}
								}
							};
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

	/** Intercepts all requests. Authenticates. */
	private class AnonymousFilter extends HttpFilter {
		@Override
		public void doFilter(HttpSession httpSession,
				final HttpServletRequest request,
				final HttpServletResponse response,
				final FilterChain filterChain) throws IOException,
				ServletException {

			// Authenticate from session
			// if (isSessionAuthenticated(httpSession)) {
			// filterChain.doFilter(request, response);
			// return;
			// }

			Subject subject = KernelUtils.anonymousLogin();
			try {
				Subject.doAs(subject, new PrivilegedExceptionAction<Void>() {
					public Void run() throws IOException, ServletException {
						filterChain.doFilter(request, response);
						return null;
					}
				});
			} catch (PrivilegedActionException e) {
				if (e.getCause() instanceof ServletException)
					throw (ServletException) e.getCause();
				else if (e.getCause() instanceof IOException)
					throw (IOException) e.getCause();
				else
					throw new CmsException("Unexpected exception", e.getCause());
			}
		}
	}

	/** Intercepts all requests. Authenticates. */
	private class DavFilter extends HttpFilter {

		@Override
		public void doFilter(final HttpSession httpSession,
				final HttpServletRequest request,
				final HttpServletResponse response,
				final FilterChain filterChain) throws IOException,
				ServletException {

			AccessControlContext acc = (AccessControlContext) httpSession
					.getAttribute(AuthConstants.ACCESS_CONTROL_CONTEXT);
			final Subject subject;
			if (acc != null) {
				subject = Subject.getSubject(acc);
			} else {
				// Process basic auth
				String basicAuth = request.getHeader(HEADER_AUTHORIZATION);
				if (basicAuth != null) {
					CallbackHandler token = basicAuth(basicAuth);
					try {
						LoginContext lc = new LoginContext(
								AuthConstants.LOGIN_CONTEXT_USER, token);
						lc.login();
						subject = lc.getSubject();
					} catch (LoginException e) {
						throw new CmsException("Could not login", e);
					}
				} else {
					requestBasicAuth(httpSession, response);
					return;
				}
			}
			// do filter as subject
			try {
				Subject.doAs(subject, new PrivilegedExceptionAction<Void>() {
					public Void run() throws IOException, ServletException {
						// add security context to session
						httpSession.setAttribute(ACCESS_CONTROL_CONTEXT,
								AccessController.getContext());
						filterChain.doFilter(request, response);
						return null;
					}
				});
			} catch (PrivilegedActionException e) {
				if (e.getCause() instanceof ServletException)
					throw (ServletException) e.getCause();
				else if (e.getCause() instanceof IOException)
					throw (IOException) e.getCause();
				else
					throw new CmsException("Unexpected exception", e.getCause());
			}

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
