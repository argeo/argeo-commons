package org.argeo.cms.security;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;

import org.argeo.api.cms.CmsConstants;

public class NodeSecurityUtils {
	public final static LdapName ROLE_ADMIN_NAME, ROLE_DATA_ADMIN_NAME, ROLE_ANONYMOUS_NAME, ROLE_USER_NAME,
			ROLE_USER_ADMIN_NAME;
	public final static List<LdapName> RESERVED_ROLES;
	static {
		try {
			ROLE_ADMIN_NAME = new LdapName(CmsConstants.ROLE_ADMIN);
			ROLE_DATA_ADMIN_NAME = new LdapName(CmsConstants.ROLE_DATA_ADMIN);
			ROLE_USER_NAME = new LdapName(CmsConstants.ROLE_USER);
			ROLE_USER_ADMIN_NAME = new LdapName(CmsConstants.ROLE_USER_ADMIN);
			ROLE_ANONYMOUS_NAME = new LdapName(CmsConstants.ROLE_ANONYMOUS);
			RESERVED_ROLES = Collections.unmodifiableList(Arrays.asList(
					new LdapName[] { ROLE_ADMIN_NAME, ROLE_ANONYMOUS_NAME, ROLE_USER_NAME, ROLE_USER_ADMIN_NAME }));
		} catch (InvalidNameException e) {
			throw new Error("Cannot initialize login module class", e);
		}
	}

	public static void checkUserName(LdapName name) throws IllegalArgumentException {
		if (RESERVED_ROLES.contains(name))
			throw new IllegalArgumentException(name + " is a reserved name");
	}

	public static void checkImpliedPrincipalName(LdapName roleName) throws IllegalArgumentException {
//		if (ROLE_USER_NAME.equals(roleName) || ROLE_ANONYMOUS_NAME.equals(roleName))
//			throw new IllegalArgumentException(roleName + " cannot be listed as role");
	}

}
