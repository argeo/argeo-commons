package org.argeo.osgi.useradmin;

import javax.naming.ldap.LdapName;

import org.argeo.naming.LdapAttrs;
import org.argeo.naming.LdapObjs;

/**
 * Standard LDAP attributes and object classes leveraged in this implementation
 * of user admin. Named {@link LdifName} in order not to collide with
 * {@link LdapName}.
 * 
 * @deprecated Use {@link LdapAttrs} and {@link LdapObjs} instead.
 */
@Deprecated
public enum LdifName {
	// Attributes
	dn, dc, cn, sn, uid, mail, displayName, objectClass, userPassword, givenName, description, member,
	// Object classes
	inetOrgPerson, organizationalPerson, person, groupOfNames, groupOfUniqueNames, top;

	public final static String PREFIX = "ldap:";

	/** For use as XML name. */
	public String property() {
		return PREFIX + name();
	}

	public static LdifName local(String property) {
		return LdifName.valueOf(property.substring(PREFIX.length()));
	}
}
