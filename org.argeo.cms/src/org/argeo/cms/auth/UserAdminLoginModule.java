package org.argeo.cms.auth;

import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.login.CredentialNotFoundException;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;

import org.argeo.ArgeoException;
import org.osgi.framework.BundleContext;
import org.osgi.service.useradmin.Authorization;
import org.osgi.service.useradmin.User;
import org.osgi.service.useradmin.UserAdmin;

public class UserAdminLoginModule implements LoginModule, AuthConstants {
	private Subject subject;
	private Map<String, Object> sharedState;
	private CallbackHandler callbackHandler;
	private boolean isAnonymous = false;

	@SuppressWarnings("unchecked")
	@Override
	public void initialize(Subject subject, CallbackHandler callbackHandler,
			Map<String, ?> sharedState, Map<String, ?> options) {
		try {
			this.subject = subject;
			this.sharedState = (Map<String, Object>) sharedState;
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
		BundleContext bc = (BundleContext) sharedState
				.get(AuthConstants.BUNDLE_CONTEXT_KEY);
		UserAdmin userAdmin = bc.getService(bc
				.getServiceReference(UserAdmin.class));
		Authorization authorization = (Authorization) sharedState
				.get(AuthConstants.AUTHORIZATION_KEY);
		if (authorization == null)
			if (!isAnonymous) {
				// ask for username and password
				NameCallback nameCallback = new NameCallback("User");
				PasswordCallback passwordCallback = new PasswordCallback(
						"Password", false);

				// handle callbacks
				try {
					callbackHandler.handle(new Callback[] { nameCallback,
							passwordCallback });
				} catch (Exception e) {
					throw new ArgeoException("Cannot handle callbacks", e);
				}

				// create credentials
				final String username = nameCallback.getName();
				if (username == null || username.trim().equals(""))
					throw new CredentialNotFoundException(
							"No credentials provided");

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
			} else {
				authorization = userAdmin.getAuthorization(null);
			}
		subject.getPrivateCredentials().add(authorization);
		return true;
	}

	@Override
	public boolean commit() throws LoginException {
		return true;
	}

	@Override
	public boolean abort() throws LoginException {
		cleanUp();
		return true;
	}

	@Override
	public boolean logout() throws LoginException {
		cleanUp();
		return true;
	}

	private void cleanUp() {
		subject.getPrivateCredentials().removeAll(
				subject.getPrivateCredentials(Authorization.class));
		subject = null;
	}

}
