package org.argeo.cms.internal.http;

import javax.security.auth.Subject;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import org.argeo.api.cms.CmsAuth;
import org.argeo.cms.CurrentUser;
import org.argeo.cms.auth.RemoteAuthCallbackHandler;
import org.argeo.cms.auth.RemoteAuthRequest;
import org.argeo.cms.auth.RemoteAuthResponse;
import org.argeo.cms.auth.RemoteAuthUtils;
import org.argeo.cms.http.server.HttpRemoteAuthExchange;

import com.sun.net.httpserver.Authenticator;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpPrincipal;

/** An {@link Authenticator} implementation based on CMS authentication. */
public class CmsAuthenticator extends Authenticator {
	// TODO make it configurable
	private final String httpAuthRealm = "Argeo";
	private final boolean forceBasic = false;

	@Override
	public Result authenticate(HttpExchange exch) {
		HttpRemoteAuthExchange remoteAuthExchange = new HttpRemoteAuthExchange(exch);
		ClassLoader currentThreadContextClassLoader = Thread.currentThread().getContextClassLoader();
		Thread.currentThread().setContextClassLoader(CmsAuthenticator.class.getClassLoader());
		LoginContext lc;
		try {
			lc = CmsAuth.USER.newLoginContext(new RemoteAuthCallbackHandler(remoteAuthExchange, remoteAuthExchange));
			lc.login();
		} catch (LoginException e) {
			if (authIsRequired(remoteAuthExchange, remoteAuthExchange)) {
				int statusCode = RemoteAuthUtils.askForWwwAuth(remoteAuthExchange, remoteAuthExchange, httpAuthRealm,
						forceBasic);
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

		String username = CurrentUser.getUsername(subject);
		HttpPrincipal httpPrincipal = new HttpPrincipal(username, httpAuthRealm);
		return new Authenticator.Success(httpPrincipal);
	}

	protected boolean authIsRequired(RemoteAuthRequest remoteAuthRequest, RemoteAuthResponse remoteAuthResponse) {
		return true;
	}

}
