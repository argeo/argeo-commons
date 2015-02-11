package org.argeo.cms.internal.kernel;

import java.io.IOException;
import java.util.Properties;
import java.util.StringTokenizer;

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
import org.eclipse.equinox.http.servlet.ExtendedHttpService;
import org.osgi.framework.BundleContext;
import org.osgi.service.http.NamespaceException;
import org.osgi.util.tracker.ServiceTracker;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
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
	private final BundleContext bundleContext;
	private ExtendedHttpService httpService;

	// FIXME Make it more unique
	private String httpAuthRealm = "Argeo";

	// Filters
	// private final RootFilter rootFilter;

	// remoting
	private OpenInViewSessionProvider sessionProvider;
	private WebdavServlet publicWebdavServlet;
	private WebdavServlet privateWebdavServlet;
	private RemotingServlet publicRemotingServlet;
	private RemotingServlet privateRemotingServlet;

	NodeHttp(BundleContext bundleContext, JackrabbitNode node,
			NodeSecurity authenticationManager) {
		this.bundleContext = bundleContext;
		this.authenticationManager = authenticationManager;

		// Equinox dependency
		ServiceTracker<ExtendedHttpService, ExtendedHttpService> st = new ServiceTracker<ExtendedHttpService, ExtendedHttpService>(
				bundleContext, ExtendedHttpService.class, null);
		st.open();
		try {
			httpService = st.waitForService(1000);
		} catch (InterruptedException e) {
			httpService = null;
		}

		if (httpService == null)
			throw new CmsException("Could not find "
					+ ExtendedHttpService.class + " service.");

		// Filters
		// rootFilter = new RootFilter();

		// DAV
		sessionProvider = new OpenInViewSessionProvider();
		publicWebdavServlet = new WebdavServlet(node, sessionProvider);
		privateWebdavServlet = new WebdavServlet(node, sessionProvider);
		publicRemotingServlet = new RemotingServlet(node, sessionProvider);
		privateRemotingServlet = new RemotingServlet(node, sessionProvider);
	}

	void publish() {
		try {
			registerWebdavServlet(PATH_WEBDAV_PUBLIC, ALIAS_NODE, true,
					publicWebdavServlet);
			registerWebdavServlet(PATH_WEBDAV_PRIVATE, ALIAS_NODE, false,
					privateWebdavServlet);
			registerRemotingServlet(PATH_REMOTING_PUBLIC, ALIAS_NODE, true,
					publicRemotingServlet);
			registerRemotingServlet(PATH_REMOTING_PRIVATE, ALIAS_NODE, false,
					privateRemotingServlet);

			// httpService.registerFilter("/", rootFilter, null, null);
		} catch (Exception e) {
			throw new CmsException("Cannot publish HTTP services to OSGi", e);
		}
	}

	private void registerWebdavServlet(String pathPrefix, String alias,
			Boolean anonymous, WebdavServlet webdavServlet)
			throws NamespaceException, ServletException {
		String path = pathPrefix + "/" + alias;
		Properties initParameters = new Properties();
		initParameters.setProperty(WebdavServlet.INIT_PARAM_RESOURCE_CONFIG,
				KernelConstants.WEBDAV_CONFIG);
		initParameters.setProperty(
				WebdavServlet.INIT_PARAM_RESOURCE_PATH_PREFIX, path);
		httpService.registerFilter(path, anonymous ? new AnonymousFilter()
				: new DavFilter(), null, null);
		// Cast to servlet because of a weird behaviour in Eclipse
		httpService.registerServlet(path, (Servlet) webdavServlet,
				initParameters, null);
	}

	private void registerRemotingServlet(String pathPrefix, String alias,
			Boolean anonymous, RemotingServlet remotingServlet)
			throws NamespaceException, ServletException {
		String path = pathPrefix + "/" + alias;
		Properties initParameters = new Properties();
		initParameters.setProperty(
				RemotingServlet.INIT_PARAM_RESOURCE_PATH_PREFIX, path);

		// Looks like a bug in Jackrabbit remoting init
		initParameters.setProperty(RemotingServlet.INIT_PARAM_HOME,
				KernelUtils.getOsgiInstanceDir(bundleContext)
						+ "/tmp/jackrabbit");
		initParameters.setProperty(RemotingServlet.INIT_PARAM_TMP_DIRECTORY,
				"remoting");
		// Cast to servlet because of a weird behaviour in Eclipse
		httpService.registerFilter(path, anonymous ? new AnonymousFilter()
				: new DavFilter(), null, null);
		httpService.registerServlet(path, (Servlet) remotingServlet,
				initParameters, null);
	}

	private Boolean isSessionAuthenticated(HttpSession httpSession) {
		SecurityContext contextFromSession = (SecurityContext) httpSession
				.getAttribute(SPRING_SECURITY_CONTEXT_KEY);
		return contextFromSession != null;
	}

	private void requestBasicAuth(HttpSession httpSession,
			HttpServletResponse response) {
		response.setStatus(401);
		response.setHeader(HEADER_WWW_AUTHENTICATE, "basic realm=\""
				+ httpAuthRealm + "\"");
		httpSession.setAttribute(ATTR_AUTH, Boolean.TRUE);
	}

	private UsernamePasswordAuthenticationToken basicAuth(String authHeader) {
		if (authHeader != null) {
			StringTokenizer st = new StringTokenizer(authHeader);
			if (st.hasMoreTokens()) {
				String basic = st.nextToken();
				if (basic.equalsIgnoreCase("Basic")) {
					try {
						String credentials = new String(Base64.decodeBase64(st
								.nextToken()), "UTF-8");
						log.debug("Credentials: " + credentials);
						int p = credentials.indexOf(":");
						if (p != -1) {
							String login = credentials.substring(0, p).trim();
							String password = credentials.substring(p + 1)
									.trim();

							return new UsernamePasswordAuthenticationToken(
									login, password);
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

			// Authenticate from session
			if (isSessionAuthenticated(httpSession)) {
				filterChain.doFilter(request, response);
				return;
			}

			// TODO Kerberos

			// TODO Certificate

			// Process basic auth
			String basicAuth = request.getHeader(HEADER_AUTHORIZATION);
			if (basicAuth != null) {
				UsernamePasswordAuthenticationToken token = basicAuth(basicAuth);
				Authentication auth = authenticationManager.authenticate(token);
				SecurityContextHolder.getContext().setAuthentication(auth);
				httpSession.setAttribute(SPRING_SECURITY_CONTEXT_KEY,
						SecurityContextHolder.getContext());
				httpSession.setAttribute(ATTR_AUTH, Boolean.FALSE);
				filterChain.doFilter(request, response);
				return;
			}

			Boolean doBasicAuth = true;
			if (doBasicAuth) {
				requestBasicAuth(httpSession, response);
				// skip filter chain
				return;
			}

			// TODO Login page

			// Anonymous
			KernelUtils.anonymousLogin(authenticationManager);
			filterChain.doFilter(request, response);
		}
	}

	/** Intercepts all requests. Authenticates. */
	class AnonymousFilter extends HttpFilter {
		@Override
		public void doFilter(HttpSession httpSession,
				HttpServletRequest request, HttpServletResponse response,
				FilterChain filterChain) throws IOException, ServletException {

			// Authenticate from session
			if (isSessionAuthenticated(httpSession)) {
				filterChain.doFilter(request, response);
				return;
			}

			KernelUtils.anonymousLogin(authenticationManager);
			filterChain.doFilter(request, response);
		}
	}

	/** Intercepts all requests. Authenticates. */
	class DavFilter extends HttpFilter {

		@Override
		public void doFilter(HttpSession httpSession,
				HttpServletRequest request, HttpServletResponse response,
				FilterChain filterChain) throws IOException, ServletException {

			// Authenticate from session
			if (isSessionAuthenticated(httpSession)) {
				filterChain.doFilter(request, response);
				return;
			}

			// Process basic auth
			String basicAuth = request.getHeader(HEADER_AUTHORIZATION);
			if (basicAuth != null) {
				UsernamePasswordAuthenticationToken token = basicAuth(basicAuth);
				Authentication auth = authenticationManager.authenticate(token);
				SecurityContextHolder.getContext().setAuthentication(auth);
				httpSession.setAttribute(SPRING_SECURITY_CONTEXT_KEY,
						SecurityContextHolder.getContext());
				httpSession.setAttribute(ATTR_AUTH, Boolean.FALSE);
				filterChain.doFilter(request, response);
				return;
			}

			requestBasicAuth(httpSession, response);
		}
	}

}
