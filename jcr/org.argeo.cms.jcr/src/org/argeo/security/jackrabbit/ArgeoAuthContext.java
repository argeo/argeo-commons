package org.argeo.security.jackrabbit;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import org.apache.jackrabbit.core.security.authentication.AuthContext;

/** Wraps a regular {@link LoginContext}, using the proper class loader. */
class ArgeoAuthContext implements AuthContext {
	private LoginContext lc;

	public ArgeoAuthContext(String appName, Subject subject, CallbackHandler callbackHandler) {
		try {
			lc = new LoginContext(appName, subject, callbackHandler);
		} catch (LoginException e) {
			throw new IllegalStateException("Cannot configure Jackrabbit login context", e);
		}
	}

	@Override
	public void login() throws LoginException {
		lc.login();
	}

	@Override
	public Subject getSubject() {
		return lc.getSubject();
	}

	@Override
	public void logout() throws LoginException {
		lc.logout();
	}

}
