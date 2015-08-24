package org.argeo.osgi.useradmin;

import java.util.Dictionary;
import java.util.Hashtable;

import javax.naming.directory.Attributes;
import javax.naming.ldap.LdapName;

import org.osgi.service.useradmin.User;
import org.osgi.service.useradmin.UserAdmin;

class LdifUser implements User {
	private final LdapName dn;
	private final Attributes attributes;

	LdifUser(LdapName dn, Attributes attributes) {
		this.dn = dn;
		this.attributes = attributes;
	}

	@Override
	public String getName() {
		return dn.toString();
	}

	@Override
	public int getType() {
		return USER;
	}

	@Override
	public Dictionary<String, Object> getProperties() {
		if (attributes == null)
			throw new ArgeoUserAdminException(
					"Must be loaded from user admin service");
		return new AttributeDictionary(attributes);
	}

	@Override
	public Dictionary<String, Object> getCredentials() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean hasCredential(String key, Object value) {
		// TODO Auto-generated method stub
		return false;
	}

	protected LdapName getDn() {
		return dn;
	}

	protected Attributes getAttributes() {
		return attributes;
	}

}
