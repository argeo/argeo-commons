package org.argeo.osgi.useradmin;

import javax.naming.ldap.LdapName;

import org.argeo.util.directory.ldap.AbstractLdapDirectory;
import org.argeo.util.directory.ldap.DefaultLdapEntry;

/** Directory user implementation */
class LdifUser extends DefaultLdapEntry implements DirectoryUser {
	LdifUser(AbstractLdapDirectory userAdmin, LdapName dn) {
		super(userAdmin, dn);
	}

	@Override
	public String getName() {
		return getDn().toString();
	}

	@Override
	public int getType() {
		return USER;
	}

}
