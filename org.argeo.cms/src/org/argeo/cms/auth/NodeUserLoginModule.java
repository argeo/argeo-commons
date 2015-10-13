package org.argeo.cms.auth;

import java.security.Principal;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;
import javax.security.auth.x500.X500Principal;

import org.apache.jackrabbit.core.security.AnonymousPrincipal;
import org.apache.jackrabbit.core.security.SecurityConstants;
import org.apache.jackrabbit.core.security.principal.AdminPrincipal;
import org.argeo.cms.CmsException;
import org.argeo.cms.internal.auth.ImpliedByPrincipal;
import org.osgi.service.useradmin.Authorization;

public class NodeUserLoginModule implements LoginModule {
	private Subject subject;

	private final static LdapName ROLE_KERNEL_NAME, ROLE_ADMIN_NAME,
			ROLE_ANONYMOUS_NAME, ROLE_USER_NAME;
	private final static List<LdapName> RESERVED_ROLES;
	private final static X500Principal ROLE_ANONYMOUS_PRINCIPAL;
	static {
		try {
			ROLE_KERNEL_NAME = new LdapName(AuthConstants.ROLE_KERNEL);
			ROLE_ADMIN_NAME = new LdapName(AuthConstants.ROLE_ADMIN);
			ROLE_USER_NAME = new LdapName(AuthConstants.ROLE_USER);
			ROLE_ANONYMOUS_NAME = new LdapName(AuthConstants.ROLE_ANONYMOUS);
			RESERVED_ROLES = Collections.unmodifiableList(Arrays
					.asList(new LdapName[] { ROLE_KERNEL_NAME, ROLE_ADMIN_NAME,
							ROLE_ANONYMOUS_NAME, ROLE_USER_NAME,
							new LdapName(AuthConstants.ROLE_GROUP_ADMIN),
							new LdapName(AuthConstants.ROLE_USER_ADMIN) }));
			ROLE_ANONYMOUS_PRINCIPAL = new X500Principal(
					ROLE_ANONYMOUS_NAME.toString());
		} catch (InvalidNameException e) {
			throw new Error("Cannot initialize login module class", e);
		}
	}

	private Authorization authorization;

	@Override
	public void initialize(Subject subject, CallbackHandler callbackHandler,
			Map<String, ?> sharedState, Map<String, ?> options) {
		this.subject = subject;
	}

	@Override
	public boolean login() throws LoginException {
		Iterator<Authorization> auth = subject.getPrivateCredentials(
				Authorization.class).iterator();
		if (!auth.hasNext())
			return false;
		authorization = auth.next();
		return true;
	}

	@Override
	public boolean commit() throws LoginException {
		if (authorization == null)
			throw new LoginException("Authorization should not be null");
		Set<Principal> principals = subject.getPrincipals();
		try {
			String authName = authorization.getName();

			// determine user's principal
			final LdapName name;
			final Principal userPrincipal;
			if (authName == null) {
				name = ROLE_ANONYMOUS_NAME;
				userPrincipal = ROLE_ANONYMOUS_PRINCIPAL;
				principals.add(userPrincipal);
				principals.add(new AnonymousPrincipal());
			} else {
				name = new LdapName(authName);
				checkUserName(name);
				userPrincipal = new X500Principal(name.toString());
				principals.add(userPrincipal);
				principals.add(new ImpliedByPrincipal(ROLE_USER_NAME,
						userPrincipal));
			}

			// Add roles provided by authorization
			for (String role : authorization.getRoles()) {
				LdapName roleName = new LdapName(role);
				if (roleName.equals(name)) {
					// skip
				} else {
					checkImpliedPrincipalName(roleName);
					principals.add(new ImpliedByPrincipal(roleName.toString(),
							userPrincipal));
					if (roleName.equals(ROLE_ADMIN_NAME))
						principals.add(new AdminPrincipal(
								SecurityConstants.ADMIN_ID));
				}
			}

			return true;
		} catch (InvalidNameException e) {
			throw new CmsException("Cannot commit", e);
		}
	}

	@Override
	public boolean abort() throws LoginException {
		cleanUp();
		return true;
	}

	@Override
	public boolean logout() throws LoginException {
		if (subject == null)
			throw new LoginException("Subject should not be null");
		// Argeo
		subject.getPrincipals().removeAll(
				subject.getPrincipals(X500Principal.class));
		subject.getPrincipals().removeAll(
				subject.getPrincipals(ImpliedByPrincipal.class));
		// Jackrabbit
		subject.getPrincipals().removeAll(
				subject.getPrincipals(AdminPrincipal.class));
		subject.getPrincipals().removeAll(
				subject.getPrincipals(AnonymousPrincipal.class));
		cleanUp();
		return true;
	}

	private void cleanUp() {
		subject = null;
		authorization = null;
	}

	private void checkUserName(LdapName name) {
		if (RESERVED_ROLES.contains(name))
			throw new CmsException(name + " is a reserved name");
	}

	private void checkImpliedPrincipalName(LdapName roleName) {
		if (ROLE_USER_NAME.equals(roleName)
				|| ROLE_ANONYMOUS_NAME.equals(roleName)
				|| ROLE_KERNEL_NAME.equals(roleName))
			throw new CmsException(roleName + " cannot be listed as role");
	}
}
