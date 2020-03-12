package org.argeo.api;

import javax.naming.ldap.LdapName;

/** The structured data */
public interface NodeInstance {
	/**
	 * To be used as an identifier of a workgroup, typically as a value for the
	 * 'businessCategory' attribute in LDAP.
	 */
	public final static String WORKGROUP = "workgroup";

	/** Mark this group as a workgroup */
	void createWorkgroup(LdapName groupDn);
}
