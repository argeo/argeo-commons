package org.argeo.cms.osgi.useradmin;

import javax.naming.ldap.LdapName;

import org.argeo.api.acr.ldap.LdapAttr;
import org.argeo.cms.auth.UserAdminUtils;
import org.argeo.cms.directory.ldap.AbstractLdapDirectory;
import org.argeo.cms.directory.ldap.DefaultLdapEntry;
import org.argeo.cms.util.LangUtils;

/** Directory user implementation */
class LdifUser extends DefaultLdapEntry implements CmsOsgiUser {
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

	public String getDisplayName() {
		String dName = getPropertyAsString(LdapAttr.displayName);
		if (LangUtils.isEmpty(dName))
			dName = getPropertyAsString(LdapAttr.cn);
		if (LangUtils.isEmpty(dName))
			dName = getPropertyAsString(LdapAttr.uid);
		if (LangUtils.isEmpty(dName))
			dName = UserAdminUtils.getUserLocalId(getName());
		return dName;
	}

}
