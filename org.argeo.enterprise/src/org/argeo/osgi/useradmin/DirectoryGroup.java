package org.argeo.osgi.useradmin;

import java.util.List;

import javax.naming.ldap.LdapName;

import org.osgi.service.useradmin.Group;

/** A group in a user directroy. */
interface DirectoryGroup extends Group, DirectoryUser {
	List<LdapName> getMemberNames();
}
