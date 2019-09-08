package org.argeo.node;

import java.util.Map;

import javax.security.auth.AuthPermission;
import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;

import org.argeo.node.security.DataAdminPrincipal;

/**
 * Log-in a system process as data admin. Protection is via
 * {@link AuthPermission} on this login module, so if it can be accessed it will
 * always succeed.
 */
public class DataAdminLoginModule implements LoginModule {
	private Subject subject;

	@Override
	public void initialize(Subject subject, CallbackHandler callbackHandler, Map<String, ?> sharedState,
			Map<String, ?> options) {
		this.subject = subject;
	}

	@Override
	public boolean login() throws LoginException {
		return true;
	}

	@Override
	public boolean commit() throws LoginException {
		subject.getPrincipals().add(new DataAdminPrincipal());
		return true;
	}

	@Override
	public boolean abort() throws LoginException {
		return true;
	}

	@Override
	public boolean logout() throws LoginException {
		subject.getPrincipals().removeAll(subject.getPrincipals(DataAdminPrincipal.class));
		return true;
	}
}
