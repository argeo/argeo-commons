package org.argeo.jackrabbit;

import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;

import org.apache.jackrabbit.core.security.SecurityConstants;
import org.apache.jackrabbit.core.security.principal.AdminPrincipal;

@Deprecated
public class JackrabbitAdminLoginModule implements LoginModule {
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
		subject.getPrincipals().add(
				new AdminPrincipal(SecurityConstants.ADMIN_ID));
		return true;
	}

	@Override
	public boolean abort() throws LoginException {
		return true;
	}

	@Override
	public boolean logout() throws LoginException {
		subject.getPrincipals().removeAll(
				subject.getPrincipals(AdminPrincipal.class));
		return true;
	}

}
