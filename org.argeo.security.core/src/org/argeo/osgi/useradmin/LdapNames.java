package org.argeo.osgi.useradmin;

/**
 * Standard LDAP attributes and object classes leverages in ths implementation
 * of user admin.
 */
public interface LdapNames {
	public final static String LDAP_PREFIX = "ldap:";
	
	// Attributes
	public final static String LDAP_CN = LDAP_PREFIX + "cn";
	public final static String LDAP_UID = LDAP_PREFIX + "uid";
	public final static String LDAP_DISPLAY_NAME = LDAP_PREFIX + "displayName";
}
