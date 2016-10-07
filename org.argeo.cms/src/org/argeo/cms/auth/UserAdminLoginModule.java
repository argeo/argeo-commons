package org.argeo.cms.auth;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.LanguageCallback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.CredentialNotFoundException;
import javax.security.auth.login.FailedLoginException;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;

import org.argeo.cms.CmsException;
import org.argeo.eclipse.ui.specific.UiContext;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.useradmin.Authorization;
import org.osgi.service.useradmin.User;
import org.osgi.service.useradmin.UserAdmin;

public class UserAdminLoginModule implements LoginModule, AuthConstants {
	private Subject subject;
	private CallbackHandler callbackHandler;
	private Map<String, Object> sharedState = null;

	private boolean isAnonymous = false;

	// private state
	private BundleContext bc;
	private Authorization authorization;

	@SuppressWarnings("unchecked")
	@Override
	public void initialize(Subject subject, CallbackHandler callbackHandler, Map<String, ?> sharedState,
			Map<String, ?> options) {
		this.subject = subject;
		try {
			bc = FrameworkUtil.getBundle(UserAdminLoginModule.class).getBundleContext();
			assert bc != null;
			// this.subject = subject;
			this.callbackHandler = callbackHandler;
			this.sharedState = (Map<String, Object>) sharedState;
			if (options.containsKey("anonymous"))
				isAnonymous = Boolean.parseBoolean(options.get("anonymous").toString());
		} catch (Exception e) {
			throw new CmsException("Cannot initialize login module", e);
		}
	}

	@Override
	public boolean login() throws LoginException {
		UserAdmin userAdmin = bc.getService(bc.getServiceReference(UserAdmin.class));
		if (isAnonymous) {
			authorization = userAdmin.getAuthorization(null);
		} else {
			// ask for username and password
			NameCallback nameCallback = new NameCallback("User");
			PasswordCallback passwordCallback = new PasswordCallback("Password", false);
			LanguageCallback langCallback = new LanguageCallback();
			try {
				callbackHandler.handle(new Callback[] { nameCallback, passwordCallback, langCallback });
			} catch (IOException e) {
				throw new LoginException("Cannot handle callback: " + e.getMessage());
				// } catch (ThreadDeath e) {
				// throw new ThreadDeathLoginException("Callbackhandler thread
				// died", e);
			} catch (UnsupportedCallbackException e) {
				return false;
			}

			// i18n
			Locale locale = langCallback.getLocale();
			if (locale == null)
				locale = Locale.getDefault();
			UiContext.setLocale(locale);

			authorization = (Authorization) sharedState.get(SHARED_STATE_AUTHORIZATION);

			if (authorization == null) {
				// create credentials
				final String username = nameCallback.getName();
				if (username == null || username.trim().equals("")) {
					// authorization = userAdmin.getAuthorization(null);
					throw new CredentialNotFoundException("No credentials provided");
				} else {
					char[] password = {};
					if (passwordCallback.getPassword() != null)
						password = passwordCallback.getPassword();
					else
						throw new CredentialNotFoundException("No credentials provided");

					User user = userAdmin.getUser(null, username);
					if (user == null)
						throw new FailedLoginException("Invalid credentials");
					if (!user.hasCredential(null, password))
						throw new FailedLoginException("Invalid credentials");
					// return false;

					// Log and monitor new login
					// if (log.isDebugEnabled())
					// log.debug("Logged in to CMS with username [" + username +
					// "]");

					authorization = userAdmin.getAuthorization(user);
				}
			}
		}
		// if (!sharedState.containsKey(SHARED_STATE_AUTHORIZATION))
		// sharedState.put(SHARED_STATE_AUTHORIZATION, authorization);
		return authorization != null;
	}

	@Override
	public boolean commit() throws LoginException {
		// Set<KerberosPrincipal> kerberosPrincipals =
		// subject.getPrincipals(KerberosPrincipal.class);
		// if (kerberosPrincipals.size() != 0) {
		// KerberosPrincipal kerberosPrincipal =
		// kerberosPrincipals.iterator().next();
		// System.out.println(kerberosPrincipal);
		// UserAdmin userAdmin =
		// bc.getService(bc.getServiceReference(UserAdmin.class));
		// User user = userAdmin.getUser(null, kerberosPrincipal.getName());
		// Authorization authorization = userAdmin.getAuthorization(user);
		// sharedState.put(SHARED_STATE_AUTHORIZATION, authorization);
		// }
		if (authorization == null) {
			return false;
			// throw new LoginException("Authorization should not be null");
		} else {
			CmsAuthUtils.addAuthentication(subject, authorization);
			return true;
		}
	}

	@Override
	public boolean abort() throws LoginException {
		authorization = null;
		return true;
	}

	@Override
	public boolean logout() throws LoginException {
		CmsAuthUtils.cleanUp(subject);
		return true;
	}
}
