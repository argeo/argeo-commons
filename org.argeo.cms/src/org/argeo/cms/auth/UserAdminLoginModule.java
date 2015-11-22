package org.argeo.cms.auth;

import java.io.IOException;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.LanguageCallback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.CredentialNotFoundException;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.argeo.ArgeoException;
import org.argeo.cms.internal.kernel.Activator;
import org.argeo.eclipse.ui.specific.UiContext;
import org.osgi.framework.BundleContext;
import org.osgi.service.http.HttpContext;
import org.osgi.service.useradmin.Authorization;
import org.osgi.service.useradmin.User;
import org.osgi.service.useradmin.UserAdmin;

public class UserAdminLoginModule implements LoginModule, AuthConstants {
	private Subject subject;
	private CallbackHandler callbackHandler;
	private boolean isAnonymous = false;

	private HttpServletRequest request = null;

	@Override
	public void initialize(Subject subject, CallbackHandler callbackHandler,
			Map<String, ?> sharedState, Map<String, ?> options) {
		try {
			this.subject = subject;
			this.callbackHandler = callbackHandler;
			if (options.containsKey("anonymous"))
				isAnonymous = Boolean.parseBoolean(options.get("anonymous")
						.toString());
		} catch (Exception e) {
			throw new ArgeoException("Cannot initialize login module", e);
		}
	}

	@Override
	public boolean login() throws LoginException {
		BundleContext bc = Activator.getBundleContext();
		UserAdmin userAdmin = bc.getService(bc
				.getServiceReference(UserAdmin.class));
		Authorization authorization = null;
		if (isAnonymous) {
			authorization = userAdmin.getAuthorization(null);
		} else {
			HttpRequestCallback httpCallback = new HttpRequestCallback();
			// ask for username and password
			NameCallback nameCallback = new NameCallback("User");
			PasswordCallback passwordCallback = new PasswordCallback(
					"Password", false);
			LanguageCallback langCallback = new LanguageCallback();
			try {
				callbackHandler.handle(new Callback[] { httpCallback,
						nameCallback, passwordCallback, langCallback });
			} catch (IOException e) {
				throw new LoginException("Cannot handle http callback: "
						+ e.getMessage());
			} catch (ThreadDeath e) {
				throw new ThreadDeathLoginException(
						"Callbackhandler thread died", e);
			} catch (UnsupportedCallbackException e) {
				return false;
			}
			request = httpCallback.getRequest();
			if (request != null) {
				authorization = (Authorization) request
						.getAttribute(HttpContext.AUTHORIZATION);
				if (authorization == null)
					authorization = (Authorization) request.getSession()
							.getAttribute(HttpContext.AUTHORIZATION);
			}

			// i18n
			Locale locale = langCallback.getLocale();
			if (locale == null)
				locale = Locale.getDefault();
			UiContext.setLocale(locale);

			if (authorization == null) {
				// create credentials
				final String username = nameCallback.getName();
				if (username == null || username.trim().equals("")) {
					// authorization = userAdmin.getAuthorization(null);
					throw new CredentialNotFoundException(
							"No credentials provided");
				} else {
					char[] password = {};
					if (passwordCallback.getPassword() != null)
						password = passwordCallback.getPassword();
					else
						throw new CredentialNotFoundException(
								"No credentials provided");

					User user = userAdmin.getUser(null, username);
					if (user == null)
						return false;
					if (!user.hasCredential(null, password))
						return false;
					authorization = userAdmin.getAuthorization(user);
				}
			}
			// } else {
			// authorization = userAdmin.getAuthorization(null);
			// }
		}
		subject.getPrivateCredentials().add(authorization);
		return true;
	}

	@Override
	public boolean commit() throws LoginException {
		Authorization authorization = subject
				.getPrivateCredentials(Authorization.class).iterator().next();
		if (request != null && authorization.getName() != null) {
			request.setAttribute(HttpContext.REMOTE_USER,
					authorization.getName());
			request.setAttribute(HttpContext.AUTHORIZATION, authorization);
			request.getSession().setAttribute(HttpContext.AUTHORIZATION,
					authorization);
			subject.getPrivateCredentials().add(request.getSession());
		}
		return true;
	}

	@Override
	public boolean abort() throws LoginException {
		cleanUp();
		return true;
	}

	@Override
	public boolean logout() throws LoginException {
		Set<HttpSession> httpSession = subject
				.getPrivateCredentials(HttpSession.class);
		Iterator<HttpSession> it = httpSession.iterator();
		while (it.hasNext()) {
			HttpSession sess = it.next();
			sess.setAttribute(HttpContext.AUTHORIZATION, null);
			// sess.setMaxInactiveInterval(1);// invalidate session
		}
		subject.getPrivateCredentials().removeAll(httpSession);
		cleanUp();
		return true;
	}

	private void cleanUp() {
		subject.getPrivateCredentials().removeAll(
				subject.getPrivateCredentials(Authorization.class));
		subject = null;
	}

}
