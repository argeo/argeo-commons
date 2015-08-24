package org.argeo.osgi.useradmin;

import java.io.InputStream;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.naming.InvalidNameException;
import javax.naming.NamingEnumeration;
import javax.naming.directory.Attributes;
import javax.naming.ldap.LdapName;

import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.useradmin.Authorization;
import org.osgi.service.useradmin.Role;
import org.osgi.service.useradmin.User;
import org.osgi.service.useradmin.UserAdmin;

public class LdifUserAdmin implements UserAdmin {
	private SortedMap<LdapName, Role> roles = new TreeMap<LdapName, Role>();

	public LdifUserAdmin(InputStream in) {
		try {
			LdifParser ldifParser = new LdifParser();
			SortedMap<LdapName, Attributes> allEntries = ldifParser.read(in);
			for (LdapName key : allEntries.keySet()) {
				Attributes attributes = allEntries.get(key);
				NamingEnumeration objectClasses = attributes.get("objectClass")
						.getAll();
				objectClasses: while (objectClasses.hasMore()) {
					String objectClass = objectClasses.next().toString();
					if (objectClass.equals("inetOrgPerson")) {
						roles.put(key, new LdifUser(key, attributes));
						break objectClasses;
					} else if (objectClass.equals("groupOfNames")) {
						roles.put(key, new LdifGroup(key, attributes));
						break objectClasses;
					}
				}
			}
		} catch (Exception e) {
			throw new ArgeoUserAdminException(
					"Cannot initialise user admin service from LDIF", e);
		}
	}

	@Override
	public Role getRole(String name) {
		LdapName key;
		try {
			key = new LdapName(name);
		} catch (InvalidNameException e) {
			// TODO implements default base DN
			throw new IllegalArgumentException("Badly formatted role name: "
					+ name, e);
		}

		if (!roles.containsKey(key))
			return null;
		return roles.get(key);
	}

	@Override
	public Authorization getAuthorization(User user) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Role createRole(String name, int type) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean removeRole(String name) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Role[] getRoles(String filter) throws InvalidSyntaxException {
		throw new UnsupportedOperationException();
	}

	@Override
	public User getUser(String key, String value) {
		throw new UnsupportedOperationException();
	}

}
