package org.argeo.cms.internal.http;

import javax.security.auth.Subject;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import org.argeo.api.cms.CmsAuth;
import org.argeo.api.cms.CmsLog;
import org.argeo.cms.auth.CurrentUser;
import org.argeo.cms.auth.RemoteAuthCallbackHandler;
import org.argeo.cms.auth.RemoteAuthRequest;
import org.argeo.cms.auth.RemoteAuthResponse;
import org.argeo.cms.auth.RemoteAuthUtils;
import org.argeo.util.CurrentSubject;

import com.sun.net.httpserver.Authenticator;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpPrincipal;

public class CmsAuthenticator extends Authenticator {
//	final static String HEADER_AUTHORIZATION = "Authorization";
//	final static String HEADER_WWW_AUTHENTICATE = "WWW-Authenticate";

	private final static CmsLog log = CmsLog.getLog(CmsAuthenticator.class);

	// TODO make it configurable
	private final String httpAuthRealm = "Argeo";
	private final boolean forceBasic = false;

	@Override
	public Result authenticate(HttpExchange exch) {
//		if (log.isTraceEnabled())
//			HttpUtils.logRequestHeaders(log, request);
		RemoteAuthHttpExchange remoteAuthExchange = new RemoteAuthHttpExchange(exch);
		ClassLoader currentThreadContextClassLoader = Thread.currentThread().getContextClassLoader();
		Thread.currentThread().setContextClassLoader(CmsAuthenticator.class.getClassLoader());
		LoginContext lc;
		try {
			lc = CmsAuth.USER.newLoginContext(new RemoteAuthCallbackHandler(remoteAuthExchange, remoteAuthExchange));
			lc.login();
		} catch (LoginException e) {
			// FIXME better analyse failure so as not to try endlessly
			if (authIsRequired(remoteAuthExchange,remoteAuthExchange)) {
				int statusCode = RemoteAuthUtils.askForWwwAuth(remoteAuthExchange, httpAuthRealm, forceBasic);
				return new Authenticator.Retry(statusCode);

			} else {
				lc = RemoteAuthUtils.anonymousLogin(remoteAuthExchange, remoteAuthExchange);
			}
			if (lc == null)
				return new Authenticator.Failure(403);
		} finally {
			Thread.currentThread().setContextClassLoader(currentThreadContextClassLoader);
		}

		Subject subject = lc.getSubject();

		CurrentSubject.callAs(subject, () -> {
			RemoteAuthUtils.configureRequestSecurity(remoteAuthExchange);
			return null;
		});
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

	protected boolean authIsRequired(RemoteAuthRequest remoteAuthRequest,
			RemoteAuthResponse remoteAuthResponse) {
		return true;
	}

}
