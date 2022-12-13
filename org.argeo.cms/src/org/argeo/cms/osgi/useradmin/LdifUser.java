package org.argeo.cms.osgi.useradmin;

import javax.naming.ldap.LdapName;

import org.argeo.api.cms.directory.CmsUser;
import org.argeo.cms.directory.ldap.AbstractLdapDirectory;
import org.argeo.cms.directory.ldap.DefaultLdapEntry;

/** Directory user implementation */
class LdifUser extends DefaultLdapEntry implements CmsUser {
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