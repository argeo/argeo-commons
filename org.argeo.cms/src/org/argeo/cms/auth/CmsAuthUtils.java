package org.argeo.cms.auth;

import java.security.Principal;
import java.util.Set;

import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.security.auth.Subject;
import javax.security.auth.x500.X500Principal;

//import org.apache.jackrabbit.core.security.AnonymousPrincipal;
//import org.apache.jackrabbit.core.security.SecurityConstants;
//import org.apache.jackrabbit.core.security.principal.AdminPrincipal;
import org.argeo.cms.CmsException;
import org.argeo.cms.internal.auth.ImpliedByPrincipal;
import org.argeo.node.security.AnonymousPrincipal;
import org.argeo.node.security.DataAdminPrincipal;
import org.argeo.node.security.NodeSecurityUtils;
import org.osgi.service.useradmin.Authorization;

class CmsAuthUtils {
	/** From org.osgi.service.http.HttpContext */
	static final String SHARED_STATE_AUTHORIZATION = "org.osgi.service.useradmin.authorization";
	/** From com.sun.security.auth.module.*LoginModule */
	static final String SHARED_STATE_NAME = "javax.security.auth.login.name";
	/** From com.sun.security.auth.module.*LoginModule */
	static final String SHARED_STATE_PWD = "javax.security.auth.login.password";

	static void addAuthentication(Subject subject, Authorization authorization) {
		assert subject != null;
		checkSubjectEmpty(subject);
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
				name = NodeSecurityUtils.ROLE_ANONYMOUS_NAME;
				userPrincipal = new AnonymousPrincipal();
				principals.add(userPrincipal);
				// principals.add(new AnonymousPrincipal());
			} else {
				name = new LdapName(authName);
				NodeSecurityUtils.checkUserName(name);
				userPrincipal = new X500Principal(name.toString());
				principals.add(userPrincipal);
				principals.add(new ImpliedByPrincipal(NodeSecurityUtils.ROLE_USER_NAME, userPrincipal));
			}

			// Add roles provided by authorization
			for (String role : authorization.getRoles()) {
				LdapName roleName = new LdapName(role);
				if (roleName.equals(name)) {
					// skip
				} else {
					NodeSecurityUtils.checkImpliedPrincipalName(roleName);
					principals.add(new ImpliedByPrincipal(roleName.toString(), userPrincipal));
					// if (roleName.equals(ROLE_ADMIN_NAME))
					// principals.add(new
					// AdminPrincipal(SecurityConstants.ADMIN_ID));
				}
			}

		} catch (InvalidNameException e) {
			throw new CmsException("Cannot commit", e);
		}
	}

	private static void checkSubjectEmpty(Subject subject) {
		if (!subject.getPrincipals(AnonymousPrincipal.class).isEmpty())
			throw new IllegalStateException("Already logged in as anonymous: " + subject);
		if (!subject.getPrincipals(X500Principal.class).isEmpty())
			throw new IllegalStateException("Already logged in as user: " + subject);
		if (!subject.getPrincipals(DataAdminPrincipal.class).isEmpty())
			throw new IllegalStateException("Already logged in as data admin: " + subject);
		if (!subject.getPrincipals(ImpliedByPrincipal.class).isEmpty())
			throw new IllegalStateException("Already authorized: " + subject);
	}

	static void cleanUp(Subject subject) {
		// Argeo
		subject.getPrincipals().removeAll(subject.getPrincipals(X500Principal.class));
		subject.getPrincipals().removeAll(subject.getPrincipals(ImpliedByPrincipal.class));
		// Jackrabbit
		// subject.getPrincipals().removeAll(subject.getPrincipals(AdminPrincipal.class));
		// subject.getPrincipals().removeAll(subject.getPrincipals(AnonymousPrincipal.class));
	}

	// SHARED STATE KEYS
	// compatible with com.sun.security.auth.module.*LoginModule
	// public static final String SHARED_STATE_USERNAME =
	// "javax.security.auth.login.name";
	// public static final String SHARED_STATE_PASSWORD =
	// "javax.security.auth.login.password";

	private CmsAuthUtils() {

	}
}
