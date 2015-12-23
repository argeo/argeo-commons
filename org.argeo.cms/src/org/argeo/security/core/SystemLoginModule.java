package org.argeo.security.core;

import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;

import org.argeo.security.SystemAuth;

public class SystemLoginModule implements LoginModule {
	private Subject subject;

	@Override
	public void initialize(Subject subject, CallbackHandler callbackHandler,
			Map<String, ?> sharedState, Map<String, ?> options) {
		this.subject = subject;
	}

	@Override
	public boolean login() throws LoginException {
		// TODO check permission?
		return true;
	}

	@Override
	public boolean commit() throws LoginException {
		subject.getPrincipals().add(new SystemAuth());
		return true;
	}

	@Override
	public boolean abort() throws LoginException {
		return true;
	}

	@Override
	public boolean logout() throws LoginException {
		// remove ALL credentials (e.g. additional Jackrabbit credentials)
		subject.getPrincipals().clear();
		return true;
	}

}