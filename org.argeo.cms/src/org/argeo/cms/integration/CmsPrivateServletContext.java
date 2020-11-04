package org.argeo.cms.integration;

import static org.argeo.api.NodeConstants.LOGIN_CONTEXT_USER;

import java.io.IOException;
import java.security.AccessControlContext;
import java.security.PrivilegedAction;
import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.argeo.cms.auth.HttpRequestCallbackHandler;
import org.argeo.cms.servlet.ServletAuthUtils;
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
	public boolean handleSecurity(final HttpServletRequest request, HttpServletResponse response) throws IOException {
		LoginContext lc = null;

		String pathInfo = request.getPathInfo();
		String servletPath = request.getServletPath();
		if ((pathInfo != null && (servletPath + pathInfo).equals(loginPage)) || servletPath.contentEquals(loginServlet))
			return true;
		try {
			lc = new LoginContext(LOGIN_CONTEXT_USER, new HttpRequestCallbackHandler(request, response));
			lc.login();
		} catch (LoginException e) {
			lc = processUnauthorized(request, response);
			if (lc == null)
				return false;
		}
		Subject.doAs(lc.getSubject(), new PrivilegedAction<Void>() {

			@Override
			public Void run() {
				// TODO also set login context in order to log out ?
				ServletAuthUtils.configureRequestSecurity(request);
				return null;
			}

		});

		return true;
	}

	@Override
	public void finishSecurity(HttpServletRequest request, HttpServletResponse response) {
		ServletAuthUtils.clearRequestSecurity(request);
	}

	protected LoginContext processUnauthorized(HttpServletRequest request, HttpServletResponse response) {
		try {
			response.sendRedirect(loginPage);
		} catch (IOException e) {
			throw new RuntimeException("Cannot redirect to login page", e);
		}
		return null;
	}
}
