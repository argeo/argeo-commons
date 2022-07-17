package org.argeo.cms.internal.http;

import javax.security.auth.Subject;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import org.argeo.api.cms.CmsAuth;
import org.argeo.api.cms.CmsLog;
import org.argeo.cms.auth.CurrentUser;
import org.argeo.cms.auth.RemoteAuthCallbackHandler;
import org.argeo.cms.auth.SpnegoLoginModule;

import com.sun.net.httpserver.Authenticator;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpPrincipal;

public class CmsAuthenticator extends Authenticator {
	final static String HEADER_AUTHORIZATION = "Authorization";
	final static String HEADER_WWW_AUTHENTICATE = "WWW-Authenticate";

	private final static CmsLog log = CmsLog.getLog(CmsAuthenticator.class);

	// TODO make it configurable
	private final String httpAuthRealm = "Argeo";
	private final boolean forceBasic = false;

	@Override
	public Result authenticate(HttpExchange exch) {
//		if (log.isTraceEnabled())
//			HttpUtils.logRequestHeaders(log, request);
		RemoteAuthHttpExchange remoteAuthHttpExchange = new RemoteAuthHttpExchange(exch);
		ClassLoader currentThreadContextClassLoader = Thread.currentThread().getContextClassLoader();
		Thread.currentThread().setContextClassLoader(CmsAuthenticator.class.getClassLoader());
		LoginContext lc;
		try {
			lc = CmsAuth.USER
					.newLoginContext(new RemoteAuthCallbackHandler(remoteAuthHttpExchange, remoteAuthHttpExchange));
			lc.login();
		} catch (LoginException e) {
			// FIXME better analyse failure so as not to try endlessly
			if (authIsRequired(exch)) {
				return askForWwwAuth(exch);
			} else {
				lc = processUnauthorized(exch);
//			if (log.isTraceEnabled())
//				HttpUtils.logResponseHeaders(log, response);
			}
			if (lc == null)
				return new Authenticator.Failure(403);
		} finally {
			Thread.currentThread().setContextClassLoader(currentThreadContextClassLoader);
		}

		Subject subject = lc.getSubject();

//		Subject.doAs(subject, new PrivilegedAction<Void>() {
//
//			@Override
//			public Void run() {
//				// TODO also set login context in order to log out ?
//				RemoteAuthUtils.configureRequestSecurity(new ServletHttpRequest(request));
//				return null;
//			}
//
//		});
		String username = CurrentUser.getUsername(subject);
		HttpPrincipal httpPrincipal = new HttpPrincipal(username, httpAuthRealm);
		return new Authenticator.Success(httpPrincipal);
	}

	protected boolean authIsRequired(HttpExchange httpExchange) {
		return true;
	}

	protected LoginContext processUnauthorized(HttpExchange httpExchange) {

		RemoteAuthHttpExchange remoteAuthExchange = new RemoteAuthHttpExchange(httpExchange);
		// anonymous
		ClassLoader currentContextClassLoader = Thread.currentThread().getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader(CmsAuthenticator.class.getClassLoader());
			LoginContext lc = CmsAuth.ANONYMOUS
					.newLoginContext(new RemoteAuthCallbackHandler(remoteAuthExchange, remoteAuthExchange));
			lc.login();
			return lc;
		} catch (LoginException e1) {
			if (log.isDebugEnabled())
				log.error("Cannot log in as anonymous", e1);
			return null;
		} finally {
			Thread.currentThread().setContextClassLoader(currentContextClassLoader);
		}
	}

	protected Authenticator.Retry askForWwwAuth(HttpExchange httpExchange) {
		// response.setHeader(HttpUtils.HEADER_WWW_AUTHENTICATE, "basic
		// realm=\"" + httpAuthRealm + "\"");
		if (SpnegoLoginModule.hasAcceptorCredentials() && !forceBasic)// SPNEGO
			httpExchange.getResponseHeaders().set(HEADER_WWW_AUTHENTICATE, "Negotiate");
		else
			httpExchange.getResponseHeaders().set(HEADER_WWW_AUTHENTICATE, "Basic realm=\"" + httpAuthRealm + "\"");

		// response.setDateHeader("Date", System.currentTimeMillis());
		// response.setDateHeader("Expires", System.currentTimeMillis() + (24 *
		// 60 * 60 * 1000));
		// response.setHeader("Accept-Ranges", "bytes");
		// response.setHeader("Connection", "Keep-Alive");
		// response.setHeader("Keep-Alive", "timeout=5, max=97");
		// response.setContentType("text/html; charset=UTF-8");

		return new Authenticator.Retry(401);
	}

}
