package org.argeo.osgi.useradmin;

import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
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

/** User admin implementation using LDIF file(s) as backend. */
public class LdifUserAdmin implements UserAdmin {
	SortedMap<LdapName, LdifUser> users = new TreeMap<LdapName, LdifUser>();
	SortedMap<LdapName, LdifGroup> groups = new TreeMap<LdapName, LdifGroup>();

	private final boolean isReadOnly;
	private final URI uri;

	public LdifUserAdmin(String uri) {
		this(uri, true);
	}

	public LdifUserAdmin(String uri, boolean isReadOnly) {
		this.isReadOnly = isReadOnly;
		try {
			this.uri = new URI(uri);
		} catch (URISyntaxException e) {
			throw new ArgeoUserAdminException("Invalid URI " + uri, e);
		}

		if (!isReadOnly && !this.uri.getScheme().equals("file:"))
			throw new UnsupportedOperationException(this.uri.getScheme()
					+ "not supported read-write.");

		try {
			load(this.uri.toURL().openStream());
		} catch (Exception e) {
			throw new ArgeoUserAdminException("Cannot open URL " + this.uri, e);
		}
	}

	public LdifUserAdmin(InputStream in) {
		load(in);
		isReadOnly = true;
		this.uri = null;
	}

	protected void load(InputStream in) {
		try {
			LdifParser ldifParser = new LdifParser();
			SortedMap<LdapName, Attributes> allEntries = ldifParser.read(in);
			for (LdapName key : allEntries.keySet()) {
				Attributes attributes = allEntries.get(key);
				NamingEnumeration<?> objectClasses = attributes.get(
						"objectClass").getAll();
				objectClasses: while (objectClasses.hasMore()) {
					String objectClass = objectClasses.next().toString();
					if (objectClass.equals("inetOrgPerson")) {
						users.put(key, new LdifUser(key, attributes));
						break objectClasses;
					} else if (objectClass.equals("groupOfNames")) {
						groups.put(key, new LdifGroup(key, attributes));
						break objectClasses;
					}
				}
			}

			// optimise
			for (LdifGroup group : groups.values()) {
				group.loadMembers(this);
			}
		} catch (Exception e) {
			throw new ArgeoUserAdminException(
					"Cannot load user admin service from LDIF", e);
		}
	}

	public void destroy() {
		users.clear();
		users = null;
		groups.clear();
		groups = null;
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

		if (groups.containsKey(key))
			return groups.get(key);
		if (users.containsKey(key))
			return users.get(key);
		return null;
	}

	@Override
	public Authorization getAuthorization(User user) {
		return new LdifAuthorization((LdifUser) user);
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

	public boolean getIsReadOnly() {
		return isReadOnly;
	}

}
