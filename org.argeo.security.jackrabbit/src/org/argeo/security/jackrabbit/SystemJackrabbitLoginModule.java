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
import org.argeo.security.SystemAuth;

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
		Set<SystemAuth> initPrincipal = subject
				.getPrincipals(SystemAuth.class);
		if (!initPrincipal.isEmpty()) {
			subject.getPrincipals().add(
					new AdminPrincipal(SecurityConstants.ADMIN_ID));
			return true;
		}

		Set<X500Principal> userPrincipal = subject
				.getPrincipals(X500Principal.class);
		if (userPrincipal.isEmpty())
			throw new LoginException("Subject must be pre-authenticated");
		if (userPrincipal.size() > 1)
			throw new LoginException("Multiple user principals "
					+ userPrincipal);

		return true;

		// Set<Principal> principals = subject.getPrincipals();
		// if (principals.isEmpty()) {// system
		// throw new LoginException("Subject must be pre-authenticated");
		// // subject.getPrincipals().add(new AdminPrincipal("admin"));
		// // return true;
		// }
		// boolean isAdmin = false;
		// boolean isAnonymous = false;
		// // FIXME make it more generic
		// for (Principal principal : principals) {
		// if (principal.getName().equalsIgnoreCase(
		// "cn=admin,ou=roles,ou=node"))
		// isAdmin = true;
		// else if (principal.getName().equalsIgnoreCase(
		// "cn=anonymous,ou=roles,ou=node"))
		// isAnonymous = true;
		// }
		//
		// if (isAnonymous && isAdmin)
		// throw new LoginException("Cannot be admin and anonymous");
		//
		// // Add special Jackrabbit roles
		// if (isAdmin)
		// principals.add(new AdminPrincipal(SecurityConstants.ADMIN_ID));
		// if (isAnonymous)// anonymous
		// principals.add(new AnonymousPrincipal());
		// return true;
	}

	@Override
	public boolean abort() throws LoginException {
		return true;
	}

	@Override
	public boolean logout() throws LoginException {
		Set<SystemAuth> initPrincipal = subject
				.getPrincipals(SystemAuth.class);
		if (!initPrincipal.isEmpty()) {
			subject.getPrincipals(AdminPrincipal.class);
			return true;
		}
		// subject.getPrincipals().removeAll(
		// subject.getPrincipals(AdminPrincipal.class));
		return true;
	}
}
