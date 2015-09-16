package org.argeo.osgi.useradmin;

import javax.naming.ldap.LdapName;

/**
 * Standard LDAP attributes and object classes leveraged in this implementation
 * of user admin. Named {@link LdifName} in order not to collide with
 * {@link LdapName}.
 */
public enum LdifName {
	// Attributes
	cn, sn, uid, mail, displayName, objectClass, userpassword,
	// Object classes
	inetOrgPerson, organizationalPerson, person, groupOfNames, top;

	public final static String PREFIX = "ldap:";

	public String property() {
		return PREFIX + name();
	}

	public static LdifName local(String property) {
		return LdifName.valueOf(property.substring(PREFIX.length()));
	}
}
