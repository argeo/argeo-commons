package org.argeo.osgi.useradmin;

import javax.naming.directory.Attributes;
import javax.naming.ldap.LdapName;

import org.argeo.util.directory.ldap.AbstractLdapDirectory;
import org.argeo.util.directory.ldap.DefaultLdapEntry;

/** Directory user implementation */
class LdifUser extends DefaultLdapEntry implements DirectoryUser {
	LdifUser(AbstractLdapDirectory userAdmin, LdapName dn, Attributes attributes) {
		super(userAdmin, dn, attributes);
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
