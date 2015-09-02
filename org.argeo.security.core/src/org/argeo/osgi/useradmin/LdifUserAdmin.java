package org.argeo.osgi.useradmin;

import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.naming.InvalidNameException;
import javax.naming.NamingEnumeration;
import javax.naming.directory.Attributes;
import javax.naming.ldap.LdapName;

import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.useradmin.Authorization;
import org.osgi.service.useradmin.Role;
import org.osgi.service.useradmin.User;

/** User admin implementation using LDIF file(s) as backend. */
public class LdifUserAdmin extends AbstractLdapUserAdmin {
	SortedMap<LdapName, LdifUser> users = new TreeMap<LdapName, LdifUser>();
	SortedMap<LdapName, LdifGroup> groups = new TreeMap<LdapName, LdifGroup>();

	private Map<String, Map<String, LdifUser>> userIndexes = new LinkedHashMap<String, Map<String, LdifUser>>();

	public LdifUserAdmin(String uri) {
		this(uri, true);
	}

	public LdifUserAdmin(String uri, boolean isReadOnly) {
		setReadOnly(isReadOnly);
		try {
			setUri(new URI(uri));
		} catch (URISyntaxException e) {
			throw new ArgeoUserAdminException("Invalid URI " + uri, e);
		}

		if (!isReadOnly && !getUri().getScheme().equals("file:"))
			throw new UnsupportedOperationException(getUri().getScheme()
					+ "not supported read-write.");

		try {
			load(getUri().toURL().openStream());
		} catch (Exception e) {
			throw new ArgeoUserAdminException("Cannot open URL " + getUri(), e);
		}
	}

	public LdifUserAdmin(InputStream in) {
		load(in);
		setReadOnly(true);
		setUri(null);
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
			for (LdifGroup group : groups.values())
				group.loadMembers(this);

			// indexes
			for (String attr : getIndexedUserProperties())
				userIndexes.put(attr, new TreeMap<String, LdifUser>());

			for (LdifUser user : users.values()) {
				Dictionary<String, Object> properties = user.getProperties();
				for (String attr : getIndexedUserProperties()) {
					Object value = properties.get(attr);
					if (value != null) {
						LdifUser otherUser = userIndexes.get(attr).put(
								value.toString(), user);
						if (otherUser != null)
							throw new ArgeoUserAdminException("User " + user
									+ " and user " + otherUser
									+ " both have property " + attr
									+ " set to " + value);
					}
				}
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
		ArrayList<Role> res = new ArrayList<Role>();
		if (filter == null) {
			res.addAll(users.values());
			res.addAll(groups.values());
		} else {
			Filter f = FrameworkUtil.createFilter(filter);
			for (LdifUser user : users.values())
				if (f.match(user.getProperties()))
					res.add(user);
			for (LdifUser group : groups.values())
				if (f.match(group.getProperties()))
					res.add(group);
		}
		return res.toArray(new Role[res.size()]);
	}

	@Override
	public User getUser(String key, String value) {
		// TODO check value null or empty
		if (key != null) {
			if (!userIndexes.containsKey(key))
				return null;
			return userIndexes.get(key).get(value);
		}

		// Try all indexes
		List<LdifUser> collectedUsers = new ArrayList<LdifUser>(
				getIndexedUserProperties().size());
		// try dn
		LdifUser user = null;
		try {
			user = (LdifUser) getRole(value);
			if (user != null)
				collectedUsers.add(user);
		} catch (Exception e) {
			// silent
		}
		for (String attr : userIndexes.keySet()) {
			user = userIndexes.get(attr).get(value);
			if (user != null)
				collectedUsers.add(user);
		}

		if (collectedUsers.size() == 1)
			return collectedUsers.get(0);
		return null;
		// throw new UnsupportedOperationException();
	}

}
