package org.argeo.osgi.useradmin;

import javax.naming.directory.Attributes;
import javax.naming.ldap.LdapName;

import org.argeo.util.transaction.AbstractWorkingCopy;

/** Working copy for a user directory being edited. */
class DirectoryUserWorkingCopy extends AbstractWorkingCopy<DirectoryUser, Attributes, LdapName> {
	@Override
	protected LdapName getId(DirectoryUser user) {
		return user.getDn();
	}

	@Override
	protected Attributes cloneAttributes(DirectoryUser user) {
		return (Attributes) user.getAttributes().clone();
	}
}
