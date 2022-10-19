package org.argeo.cms.integration;

import java.io.IOException;
import java.security.AccessControlContext;
import java.util.Map;

import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.argeo.api.cms.CmsAuth;
import org.argeo.cms.auth.RemoteAuthCallbackHandler;
import org.argeo.cms.auth.RemoteAuthUtils;
import org.argeo.cms.servlet.ServletHttpRequest;
import org.argeo.cms.servlet.ServletHttpResponse;
import org.osgi.service.http.context.ServletContextHelper;

/** Manages security access to servlets. */
public class CmsPrivateServletContext extends ServletContextHelper {
	public final static String LOGIN_PAGE = "argeo.cms.integration.loginPage";
	public final static String LOGIN_SERVLET = "argeo.cms.integration.loginServlet";
	private String loginPage;
	private String loginServlet;

	public void init(Map<String, String> properties) {
		loginPage = properties.get(LOGIN_PAGE);
		loginServlet = properties.get(LOGIN_SERVLET);
	}

	/**
	 * Add the {@link AccessControlContext} as a request attribute, or redirect to
	 * the login page.
	 */
	@Override
	public boolean handleSecurity(final HttpServletRequest req, HttpServletResponse resp) throws IOException {
		LoginContext lc = null;
		ServletHttpRequest request = new ServletHttpRequest(req);
		ServletHttpResponse response = new ServletHttpResponse(resp);

		String pathInfo = req.getPathInfo();
		String servletPath = req.getServletPath();
		if ((pathInfo != null && (servletPath + pathInfo).equals(loginPage)) || servletPath.contentEquals(loginServlet))
			return true;
		try {
			lc = CmsAuth.USER.newLoginContext(new RemoteAuthCallbackHandler(request, response));
			lc.login();
		} catch (LoginException e) {
			lc = processUnauthorized(req, resp);
			if (lc == null)
				return false;
		}
//		Subject.doAs(lc.getSubject(), new PrivilegedAction<Void>() {
//
//			@Override
//			public Void run() {
//				// TODO also set login context in order to log out ?
//				RemoteAuthUtils.configureRequestSecurity(request);
//				return null;
//			}
//
//		});

		return true;
	}

//	@Override
//	public void finishSecurity(HttpServletRequest req, HttpServletResponse resp) {
//		RemoteAuthUtils.clearRequestSecurity(new ServletHttpRequest(req));
//	}

	protected LoginContext processUnauthorized(HttpServletRequest request, HttpServletResponse response) {
		try {
			response.sendRedirect(loginPage);
		} catch (IOException e) {
			throw new RuntimeException("Cannot redirect to login page", e);
		}
		return null;
	}
}
