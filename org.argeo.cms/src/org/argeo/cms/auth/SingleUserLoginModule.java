package org.argeo.cms.auth;

import java.security.Principal;
import java.util.Map;
import java.util.Set;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;
import javax.security.auth.x500.X500Principal;

import org.argeo.cms.internal.auth.ImpliedByPrincipal;
import org.argeo.node.NodeConstants;
import org.argeo.node.security.DataAdminPrincipal;

public class SingleUserLoginModule implements LoginModule {
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
		String username = System.getProperty("user.name");
		X500Principal principal = new X500Principal("uid=" + username + ",dc=localhost,dc=localdomain");
		Set<Principal> principals = subject.getPrincipals();
		principals.add(principal);
		principals.add(new ImpliedByPrincipal(NodeConstants.ROLE_ADMIN, principal));
		principals.add(new DataAdminPrincipal());
		return true;
	}

	@Override
	public boolean abort() throws LoginException {
		return true;
	}

	@Override
	public boolean logout() throws LoginException {
		// TODO Auto-generated method stub
		return true;
	}

}
