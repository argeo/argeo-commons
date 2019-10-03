package org.argeo.osgi.useradmin;

import javax.naming.directory.Attributes;
import javax.naming.ldap.LdapName;

import org.osgi.service.useradmin.User;

/** A user in a user directory. */
interface DirectoryUser extends User {
	LdapName getDn();

	Attributes getAttributes();

	void publishAttributes(Attributes modifiedAttributes);
}
