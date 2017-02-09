package org.argeo.node;

import javax.naming.ldap.LdapName;

/** The structured data */
public interface NodeInstance {
	/** Mark this group as a workgroup */
	void createWorkgroup(LdapName groupDn);
}
