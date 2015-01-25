package org.argeo.cms.internal.kernel;

import java.io.IOException;
import java.util.StringTokenizer;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.argeo.cms.CmsException;
import org.eclipse.equinox.http.servlet.ExtendedHttpService;
import org.osgi.framework.BundleContext;
import org.osgi.service.http.HttpContext;
import org.osgi.util.tracker.ServiceTracker;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

class NodeHttpFilter implements Filter {
	private final static Log log = LogFactory.getLog(NodeHttpFilter.class);

	private final static String ATTR_AUTH = "auth";
	private final static String HEADER_AUTHORIZATION = "Authorization";

	static final String SPRING_SECURITY_CONTEXT_KEY = "SPRING_SECURITY_CONTEXT";

	private ExtendedHttpService httpService;
	private final AuthenticationManager authenticationManager;

	private Boolean basicAuthEnabled = false;

	NodeHttpFilter(BundleContext bundleContext,
			AuthenticationManager authenticationManager) {
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
	}

	void publish() {
		try {
			HttpContext httpContext = httpService.createDefaultHttpContext();
			httpService.registerFilter("/", this, null, httpContext);
		} catch (Exception e) {
			throw new CmsException("Cannot register HTTP filter", e);
		}
	}

	@Override
	public void doFilter(ServletRequest servletRequest,
			ServletResponse servletResponse, FilterChain filterChain)
			throws IOException, ServletException {
		HttpServletRequest request = (HttpServletRequest) servletRequest;
		HttpSession httpSession = request.getSession();

		// Authenticate from session
		SecurityContext contextFromSession = (SecurityContext) httpSession
				.getAttribute(SPRING_SECURITY_CONTEXT_KEY);
		if (contextFromSession != null) {
			filterChain.doFilter(servletRequest, servletResponse);
			return;
		}

		if (basicAuthEnabled) {
			// Basic auth
			String basicAuth = request.getHeader(HEADER_AUTHORIZATION);

			// for (Enumeration<String> headerNames = request.getHeaderNames();
			// headerNames
			// .hasMoreElements();) {
			// String headerName = headerNames.nextElement();
			// Object headerValue = request.getHeader(headerName);
			// log.debug(headerName + ": " + headerValue);
			// }

			if (basicAuth == null) {
				HttpServletResponse response = (HttpServletResponse) servletResponse;
				response.setStatus(401);
				response.setHeader("WWW-Authenticate", "basic realm=\"Auth ("
						+ httpSession.getCreationTime() + ")\"");
				httpSession.setAttribute(ATTR_AUTH, Boolean.TRUE);
				return;
			} else {
				UsernamePasswordAuthenticationToken token = basicAuth(basicAuth);
				Authentication auth = authenticationManager.authenticate(token);
				SecurityContextHolder.getContext().setAuthentication(auth);
				httpSession.setAttribute(SPRING_SECURITY_CONTEXT_KEY,
						SecurityContextHolder.getContext());
				httpSession.setAttribute(ATTR_AUTH, Boolean.FALSE);
			}
		}
		// Assume authentication has been done and continue
		filterChain.doFilter(servletRequest, servletResponse);
	}

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
	}

	@Override
	public void destroy() {
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

}
