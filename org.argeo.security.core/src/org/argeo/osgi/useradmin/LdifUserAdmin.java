package org.argeo.osgi.useradmin;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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
import javax.naming.directory.BasicAttributes;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

import org.apache.commons.io.IOUtils;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.useradmin.Authorization;
import org.osgi.service.useradmin.Role;
import org.osgi.service.useradmin.User;

/** User admin implementation using LDIF file(s) as backend. */
public class LdifUserAdmin extends AbstractUserDirectory {
	SortedMap<LdapName, LdifUser> users = new TreeMap<LdapName, LdifUser>();
	SortedMap<LdapName, LdifGroup> groups = new TreeMap<LdapName, LdifGroup>();

	private Map<String, Map<String, LdifUser>> userIndexes = new LinkedHashMap<String, Map<String, LdifUser>>();

	// private Map<LdapName, List<LdifGroup>> directMemberOf = new
	// TreeMap<LdapName, List<LdifGroup>>();
	private XaRes xaRes = new XaRes();

	public LdifUserAdmin(String uri) {
		this(uri, readOnlyDefault(uri));
	}

	public LdifUserAdmin(String uri, boolean isReadOnly) {
		setReadOnly(isReadOnly);
		try {
			setUri(new URI(uri));
		} catch (URISyntaxException e) {
			throw new UserDirectoryException("Invalid URI " + uri, e);
		}

		if (!isReadOnly && !getUri().getScheme().equals("file"))
			throw new UnsupportedOperationException(getUri().getScheme()
					+ " not supported read-write.");

	}

	public LdifUserAdmin(URI uri, boolean isReadOnly) {
		setReadOnly(isReadOnly);
		setUri(uri);
		if (!isReadOnly && !getUri().getScheme().equals("file"))
			throw new UnsupportedOperationException(getUri().getScheme()
					+ " not supported read-write.");

	}

	public LdifUserAdmin(InputStream in) {
		load(in);
		setReadOnly(true);
		setUri(null);
	}

	private static boolean readOnlyDefault(String uriStr) {
		URI uri;
		try {
			uri = new URI(uriStr);
		} catch (Exception e) {
			throw new UserDirectoryException("Invalid URI " + uriStr, e);
		}
		if (uri.getScheme().equals("file")) {
			File file = new File(uri);
			return !file.canWrite();
		}
		return true;
	}

	public void init() {
		try {
			load(getUri().toURL().openStream());
		} catch (Exception e) {
			throw new UserDirectoryException("Cannot open URL " + getUri(), e);
		}
	}

	public void save() {
		if (getUri() == null || isReadOnly())
			throw new UserDirectoryException("Cannot save LDIF user admin");
		try (FileOutputStream out = new FileOutputStream(new File(getUri()))) {
			save(out);
		} catch (IOException e) {
			throw new UserDirectoryException("Cannot save user admin to "
					+ getUri(), e);
		}
	}

	public void save(OutputStream out) throws IOException {
		try {
			LdifWriter ldifWriter = new LdifWriter(out);
			for (LdapName name : groups.keySet())
				ldifWriter.writeEntry(name, groups.get(name).getAttributes());
			for (LdapName name : users.keySet())
				ldifWriter.writeEntry(name, users.get(name).getAttributes());
		} finally {
			IOUtils.closeQuietly(out);
		}
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
						users.put(key, new LdifUser(this, key, attributes));
						break objectClasses;
					} else if (objectClass.equals("groupOfNames")) {
						groups.put(key, new LdifGroup(this, key, attributes));
						break objectClasses;
					}
				}
			}

			// optimise
			// for (LdifGroup group : groups.values())
			// loadMembers(group);

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
							throw new UserDirectoryException("User " + user
									+ " and user " + otherUser
									+ " both have property " + attr
									+ " set to " + value);
					}
				}
			}
		} catch (Exception e) {
			throw new UserDirectoryException(
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
		return new LdifAuthorization((LdifUser) user,
				getAllRoles((LdifUser) user));
	}

	@Override
	public Role createRole(String name, int type) {
		try {
			LdapName dn = new LdapName(name);
			if (users.containsKey(dn) || groups.containsKey(dn))
				throw new UserDirectoryException("Already a role " + name);

			BasicAttributes attrs = new BasicAttributes();
			attrs.put("dn", dn.toString());
			Rdn nameRdn = dn.getRdn(dn.size() - 1);
			// TODO deal with multiple attr RDN
			attrs.put(nameRdn.getType(), nameRdn.getValue());
			LdifUser newRole;
			if (type == Role.USER) {
				newRole = new LdifUser(this, dn, attrs);
				users.put(dn, newRole);
			} else if (type == Role.GROUP) {
				newRole = new LdifGroup(this, dn, attrs);
				groups.put(dn, (LdifGroup) newRole);
			} else
				throw new UserDirectoryException("Unsupported type " + type);
			return newRole;
		} catch (InvalidNameException e) {
			throw new UserDirectoryException("Cannot create role " + name, e);
		}
	}

	@Override
	public boolean removeRole(String name) {
		try {
			LdapName dn = new LdapName(name);
			LdifUser role = null;
			if (users.containsKey(dn))
				role = users.remove(dn);
			else if (groups.containsKey(dn))
				role = groups.remove(dn);
			else
				throw new UserDirectoryException("There is no role " + name);
			if (role == null)
				return false;
			for (LdifGroup group : getDirectGroups(role)) {
				// group.directMembers.remove(role);
				group.getAttributes().get(getMemberAttributeId())
						.remove(dn.toString());
			}
			if (role instanceof LdifGroup) {
				LdifGroup group = (LdifGroup) role;
				// for (Role user : group.directMembers) {
				// if (user instanceof LdifUser)
				// directMemberOf.get(((LdifUser) user).getDn()).remove(
				// group);
				// }
			}
			return true;
		} catch (InvalidNameException e) {
			throw new UserDirectoryException("Cannot create role " + name, e);
		}
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

	// protected void loadMembers(LdifGroup group) {
	// group.directMembers = new ArrayList<Role>();
	// for (LdapName ldapName : group.getMemberNames()) {
	// LdifUser role = null;
	// if (groups.containsKey(ldapName))
	// role = groups.get(ldapName);
	// else if (users.containsKey(ldapName))
	// role = users.get(ldapName);
	// else {
	// if (getExternalRoles() != null)
	// role = (LdifUser) getExternalRoles().getRole(
	// ldapName.toString());
	// if (role == null)
	// throw new ArgeoUserAdminException("No role found for "
	// + ldapName);
	// }
	// // role.directMemberOf.add(group);
	// // if (!directMemberOf.containsKey(role.getDn()))
	// // directMemberOf.put(role.getDn(), new ArrayList<LdifGroup>());
	// // directMemberOf.get(role.getDn()).add(group);
	// group.directMembers.add(role);
	// }
	// }

	@Override
	protected List<LdifGroup> getDirectGroups(User user) {
		LdapName dn;
		if (user instanceof LdifUser)
			dn = ((LdifUser) user).getDn();
		else
			try {
				dn = new LdapName(user.getName());
			} catch (InvalidNameException e) {
				throw new UserDirectoryException("Badly formatted user name "
						+ user.getName(), e);
			}

		List<LdifGroup> directGroups = new ArrayList<LdifGroup>();
		for (LdapName name : groups.keySet()) {
			LdifGroup group = groups.get(name);
			if (group.getMemberNames().contains(dn))
				directGroups.add(group);
		}
		return directGroups;
		// if (directMemberOf.containsKey(dn))
		// return Collections.unmodifiableList(directMemberOf.get(dn));
		// else
		// return Collections.EMPTY_LIST;
	}

	@Override
	public XAResource getXAResource() {
		return xaRes;
	}

	private class XaRes implements XAResource {

		@Override
		public void commit(Xid xid, boolean onePhase) throws XAException {
			save();
		}

		@Override
		public void end(Xid xid, int flags) throws XAException {
			// TODO Auto-generated method stub

		}

		@Override
		public void forget(Xid xid) throws XAException {
			// TODO Auto-generated method stub

		}

		@Override
		public int getTransactionTimeout() throws XAException {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public boolean isSameRM(XAResource xares) throws XAException {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public int prepare(Xid xid) throws XAException {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public Xid[] recover(int flag) throws XAException {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public void rollback(Xid xid) throws XAException {
			// TODO Auto-generated method stub

		}

		@Override
		public boolean setTransactionTimeout(int seconds) throws XAException {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public void start(Xid xid, int flags) throws XAException {
			// TODO Auto-generated method stub

		}

	}

}
