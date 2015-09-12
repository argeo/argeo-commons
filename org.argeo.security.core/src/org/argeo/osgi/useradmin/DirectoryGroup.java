package org.argeo.osgi.useradmin;

import java.util.List;

import javax.naming.ldap.LdapName;

import org.osgi.service.useradmin.Group;

public interface DirectoryGroup extends Group, DirectoryUser {
	List<LdapName> getMemberNames();
}
