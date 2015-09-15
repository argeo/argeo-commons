package org.argeo.osgi.useradmin;

import javax.naming.ldap.LdapName;

/**
 * Standard LDAP attributes and object classes leveraged in this implementation
 * of user admin. Named {@link LdifName} in order not to collide with
 * {@link LdapName}.
 */
public enum LdifName {
	// Attributes
	cn, sn, uid, displayName, objectClass,userPassword,
	// Object classes
	inetOrgPerson, organizationalPerson, person, groupOfNames, top;

	public final static String LDAP_PREFIX = "ldap:";

	public String property() {
		return LDAP_PREFIX + name();
	}

	public static LdifName local(String property) {
		String local = property.substring(LDAP_PREFIX.length());
		return LdifName.valueOf(local);
	}
}
