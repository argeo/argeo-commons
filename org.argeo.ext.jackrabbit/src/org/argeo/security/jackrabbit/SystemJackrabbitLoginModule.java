package org.argeo.security.jackrabbit;

import java.util.Map;
import java.util.Set;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;
import javax.security.auth.x500.X500Principal;

import org.apache.jackrabbit.core.security.SecurityConstants;
import org.apache.jackrabbit.core.security.principal.AdminPrincipal;
import org.argeo.node.DataAdminPrincipal;

public class SystemJackrabbitLoginModule implements LoginModule {

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
		Set<DataAdminPrincipal> initPrincipal = subject.getPrincipals(DataAdminPrincipal.class);
		if (!initPrincipal.isEmpty()) {
			subject.getPrincipals().add(new AdminPrincipal(SecurityConstants.ADMIN_ID));
			return true;
		}

		Set<X500Principal> userPrincipal = subject.getPrincipals(X500Principal.class);
		if (userPrincipal.isEmpty())
			throw new LoginException("Subject must be pre-authenticated");
		if (userPrincipal.size() > 1)
			throw new LoginException("Multiple user principals " + userPrincipal);

		return true;
	}

	@Override
	public boolean abort() throws LoginException {
		return true;
	}

	@Override
	public boolean logout() throws LoginException {
		Set<DataAdminPrincipal> initPrincipal = subject.getPrincipals(DataAdminPrincipal.class);
		if (!initPrincipal.isEmpty()) {
			subject.getPrincipals(AdminPrincipal.class);
			return true;
		}
		return true;
	}
}
