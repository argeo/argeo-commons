package org.argeo.cms.internal.http;

import java.io.IOException;
import java.net.URL;

import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.argeo.api.NodeConstants;
import org.argeo.cms.auth.HttpRequestCallbackHandler;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.http.HttpContext;

public class DataHttpContext implements HttpContext {
	private final static Log log = LogFactory.getLog(DataHttpContext.class);

	private final BundleContext bc = FrameworkUtil.getBundle(getClass()).getBundleContext();

	// FIXME Make it more unique
	private final String httpAuthRealm;
	private final boolean forceBasic;

	public DataHttpContext(String httpAuthrealm, boolean forceBasic) {
		this.httpAuthRealm = httpAuthrealm;
		this.forceBasic = forceBasic;
	}

	public DataHttpContext(String httpAuthrealm) {
		this(httpAuthrealm, false);
	}

	@Override
	public boolean handleSecurity(final HttpServletRequest request, HttpServletResponse response) throws IOException {

		if (log.isTraceEnabled())
			HttpUtils.logRequestHeaders(log, request);
		LoginContext lc;
		try {
			lc = new LoginContext(NodeConstants.LOGIN_CONTEXT_USER, new HttpRequestCallbackHandler(request, response));
			lc.login();
		} catch (LoginException e) {
			lc = processUnauthorized(request, response);
			if (lc == null)
				return false;
		}
		return true;
	}

	@Override
	public URL getResource(String name) {
		return bc.getBundle().getResource(name);
	}

	@Override
	public String getMimeType(String name) {
		return null;
	}

	protected LoginContext processUnauthorized(HttpServletRequest request, HttpServletResponse response) {
		// anonymous
		try {
			LoginContext lc = new LoginContext(NodeConstants.LOGIN_CONTEXT_ANONYMOUS, new HttpRequestCallbackHandler(request, response));
			lc.login();
			return lc;
		} catch (LoginException e1) {
			if (log.isDebugEnabled())
				log.error("Cannot log in as anonymous", e1);
			return null;
		}
	}
	protected void askForWwwAuth(HttpServletRequest request, HttpServletResponse response) {
		response.setStatus(401);
		// response.setHeader(HttpUtils.HEADER_WWW_AUTHENTICATE, "basic
		// realm=\"" + httpAuthRealm + "\"");
		if (org.argeo.cms.internal.kernel.Activator.getAcceptorCredentials() != null && !forceBasic)// SPNEGO
			response.setHeader(HttpUtils.HEADER_WWW_AUTHENTICATE, "Negotiate");
		else
			response.setHeader(HttpUtils.HEADER_WWW_AUTHENTICATE, "Basic realm=\"" + httpAuthRealm + "\"");

		// response.setDateHeader("Date", System.currentTimeMillis());
		// response.setDateHeader("Expires", System.currentTimeMillis() + (24 *
		// 60 * 60 * 1000));
		// response.setHeader("Accept-Ranges", "bytes");
		// response.setHeader("Connection", "Keep-Alive");
		// response.setHeader("Keep-Alive", "timeout=5, max=97");
		// response.setContentType("text/html; charset=UTF-8");

	}

}
