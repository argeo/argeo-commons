package org.argeo.cms.auth;

import java.security.Principal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.security.auth.Subject;
import javax.security.auth.x500.X500Principal;

import org.apache.jackrabbit.core.security.AnonymousPrincipal;
import org.apache.jackrabbit.core.security.SecurityConstants;
import org.apache.jackrabbit.core.security.principal.AdminPrincipal;
import org.argeo.cms.CmsException;
import org.argeo.cms.internal.auth.ImpliedByPrincipal;
import org.argeo.node.NodeConstants;
import org.osgi.service.useradmin.Authorization;

class CmsAuthUtils {
	final static LdapName ROLE_ADMIN_NAME, ROLE_ANONYMOUS_NAME, ROLE_USER_NAME;
	final static List<LdapName> RESERVED_ROLES;
	final static X500Principal ROLE_ANONYMOUS_PRINCIPAL;
	static {
		try {
			// ROLE_KERNEL_NAME = new LdapName(AuthConstants.ROLE_KERNEL);
			ROLE_ADMIN_NAME = new LdapName(NodeConstants.ROLE_ADMIN);
			ROLE_USER_NAME = new LdapName(NodeConstants.ROLE_USER);
			ROLE_ANONYMOUS_NAME = new LdapName(NodeConstants.ROLE_ANONYMOUS);
			RESERVED_ROLES = Collections.unmodifiableList(Arrays.asList(new LdapName[] { ROLE_ADMIN_NAME,
					ROLE_ANONYMOUS_NAME, ROLE_USER_NAME, new LdapName(AuthConstants.ROLE_GROUP_ADMIN),
					new LdapName(NodeConstants.ROLE_USER_ADMIN) }));
			ROLE_ANONYMOUS_PRINCIPAL = new X500Principal(ROLE_ANONYMOUS_NAME.toString());
		} catch (InvalidNameException e) {
			throw new Error("Cannot initialize login module class", e);
		}
	}

	static void addAuthentication(Subject subject, Authorization authorization) {
		assert subject != null;
		assert authorization != null;
		
		// required for display name:
		subject.getPrivateCredentials().add(authorization);

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
				principals.add(new ImpliedByPrincipal(ROLE_USER_NAME, userPrincipal));
			}

			// Add roles provided by authorization
			for (String role : authorization.getRoles()) {
				LdapName roleName = new LdapName(role);
				if (roleName.equals(name)) {
					// skip
				} else {
					checkImpliedPrincipalName(roleName);
					principals.add(new ImpliedByPrincipal(roleName.toString(), userPrincipal));
					if (roleName.equals(ROLE_ADMIN_NAME))
						principals.add(new AdminPrincipal(SecurityConstants.ADMIN_ID));
				}
			}

		} catch (InvalidNameException e) {
			throw new CmsException("Cannot commit", e);
		}
	}

	static void checkUserName(LdapName name) {
		if (RESERVED_ROLES.contains(name))
			throw new CmsException(name + " is a reserved name");
	}

	static void checkImpliedPrincipalName(LdapName roleName) {
		if (ROLE_USER_NAME.equals(roleName) || ROLE_ANONYMOUS_NAME.equals(roleName))
			throw new CmsException(roleName + " cannot be listed as role");
	}
	
	static void cleanUp(Subject subject){
		// Argeo
		subject.getPrincipals().removeAll(subject.getPrincipals(X500Principal.class));
		subject.getPrincipals().removeAll(subject.getPrincipals(ImpliedByPrincipal.class));
		// Jackrabbit
		subject.getPrincipals().removeAll(subject.getPrincipals(AdminPrincipal.class));
		subject.getPrincipals().removeAll(subject.getPrincipals(AnonymousPrincipal.class));
	}

	private CmsAuthUtils() {

	}
}
