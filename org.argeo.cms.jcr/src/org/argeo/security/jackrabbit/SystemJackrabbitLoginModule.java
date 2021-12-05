package org.argeo.security.jackrabbit;

import java.util.Map;
import java.util.Set;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;
import javax.security.auth.x500.X500Principal;

import org.apache.jackrabbit.core.security.AnonymousPrincipal;
import org.apache.jackrabbit.core.security.SecurityConstants;
import org.apache.jackrabbit.core.security.principal.AdminPrincipal;
import org.argeo.api.security.DataAdminPrincipal;

/** JAAS login module used when initiating a new Jackrabbit session. */
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
		Set<org.argeo.api.security.AnonymousPrincipal> anonPrincipal = subject
				.getPrincipals(org.argeo.api.security.AnonymousPrincipal.class);
		if (!anonPrincipal.isEmpty()) {
			subject.getPrincipals().add(new AnonymousPrincipal());
			return true;
		}

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
		subject.getPrincipals().removeAll(subject.getPrincipals(AnonymousPrincipal.class));
		subject.getPrincipals().removeAll(subject.getPrincipals(AdminPrincipal.class));
		return true;
	}
}
