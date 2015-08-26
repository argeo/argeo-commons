package org.argeo.security.jackrabbit;

import java.security.Principal;
import java.util.Map;
import java.util.Set;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;

import org.apache.jackrabbit.core.security.AnonymousPrincipal;
import org.apache.jackrabbit.core.security.principal.AdminPrincipal;

public class SystemJackrabbitLoginModule implements LoginModule {

	private Subject subject;

	@Override
	public void initialize(Subject subject, CallbackHandler callbackHandler,
			Map<String, ?> sharedState, Map<String, ?> options) {
		this.subject = subject;
	}

	@Override
	public boolean login() throws LoginException {
		return true;
	}

	@Override
	public boolean commit() throws LoginException {
		Set<Principal> principals = subject.getPrincipals();
		if (principals.isEmpty()) {// system
			subject.getPrincipals().add(new AdminPrincipal("admin"));
			return true;
		}
		boolean isAdmin = false;
		boolean isAnonymous = false;
		// FIXME make it more generic
		for (Principal principal : principals) {
			if (principal.getName().equalsIgnoreCase(
					"cn=admin,ou=system,ou=node"))
				isAdmin = true;
			else if (principal.getName().equalsIgnoreCase(
					"cn=anonymous,ou=system,ou=node"))
				isAnonymous = true;
		}

		if (isAnonymous && isAdmin)
			throw new LoginException("Cannot be admin and anonymous");

		// Add special Jackrabbit roles
		if (isAdmin)
			principals.add(new AdminPrincipal("admin"));
		if (isAnonymous)// anonymous
			principals.add(new AnonymousPrincipal());
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
