package org.argeo.osgi.useradmin;

import static org.argeo.osgi.useradmin.LdifName.inetOrgPerson;
import static org.argeo.osgi.useradmin.LdifName.objectClass;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.naming.NamingEnumeration;
import javax.naming.directory.Attributes;
import javax.naming.ldap.LdapName;
import javax.transaction.TransactionManager;

import org.argeo.util.naming.LdifParser;
import org.argeo.util.naming.LdifWriter;
import org.osgi.framework.Filter;
import org.osgi.service.useradmin.Role;

/**
 * A user admin based on a LDIF files. Requires a {@link TransactionManager} and
 * an open transaction for write access.
 */
public class LdifUserAdmin extends AbstractUserDirectory {
	private SortedMap<LdapName, DirectoryUser> users = new TreeMap<LdapName, DirectoryUser>();
	private SortedMap<LdapName, DirectoryGroup> groups = new TreeMap<LdapName, DirectoryGroup>();

	public LdifUserAdmin(String uri, String baseDn) {
		this(fromUri(uri, baseDn));
	}

	public LdifUserAdmin(Dictionary<String, ?> properties) {
		super(properties);
	}

	public LdifUserAdmin(InputStream in) {
		super(new Hashtable<String, Object>());
		load(in);
	}

	private static Dictionary<String, Object> fromUri(String uri, String baseDn) {
		Hashtable<String, Object> res = new Hashtable<String, Object>();
		res.put(UserAdminConf.uri.name(), uri);
		res.put(UserAdminConf.baseDn.name(), baseDn);
		return res;
	}

	public void init() {
		try {
			if (getUri().getScheme().equals("file")) {
				File file = new File(getUri());
				if (!file.exists())
					return;
			}
			load(getUri().toURL().openStream());
		} catch (Exception e) {
			throw new UserDirectoryException("Cannot open URL " + getUri(), e);
		}
	}

	public void save() {
		if (getUri() == null)
			throw new UserDirectoryException("Cannot save LDIF user admin: no URI is set");
		if (isReadOnly())
			throw new UserDirectoryException("Cannot save LDIF user admin: " + getUri() + " is read-only");
		try (FileOutputStream out = new FileOutputStream(new File(getUri()))) {
			save(out);
		} catch (IOException e) {
			throw new UserDirectoryException("Cannot save user admin to " + getUri(), e);
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
			out.close();
		}
	}

	protected void load(InputStream in) {
		try {
			users.clear();
			groups.clear();

			LdifParser ldifParser = new LdifParser();
			SortedMap<LdapName, Attributes> allEntries = ldifParser.read(in);
			for (LdapName key : allEntries.keySet()) {
				Attributes attributes = allEntries.get(key);
				// check for inconsistency
				Set<String> lowerCase = new HashSet<String>();
				NamingEnumeration<String> ids = attributes.getIDs();
				while (ids.hasMoreElements()) {
					String id = ids.nextElement().toLowerCase();
					if (lowerCase.contains(id))
						throw new UserDirectoryException(key + " has duplicate id " + id);
					lowerCase.add(id);
				}

				// analyse object classes
				NamingEnumeration<?> objectClasses = attributes.get(objectClass.name()).getAll();
				// System.out.println(key);
				objectClasses: while (objectClasses.hasMore()) {
					String objectClass = objectClasses.next().toString();
					// System.out.println(" " + objectClass);
					if (objectClass.equals(inetOrgPerson.name())) {
						users.put(key, new LdifUser(this, key, attributes));
						break objectClasses;
					} else if (objectClass.equals(getGroupObjectClass())) {
						groups.put(key, new LdifGroup(this, key, attributes));
						break objectClasses;
					}
				}
			}
		} catch (Exception e) {
			throw new UserDirectoryException("Cannot load user admin service from LDIF", e);
		}
	}

	public void destroy() {
		if (users == null || groups == null)
			throw new UserDirectoryException("User directory " + getBaseDn() + " is already destroyed");
		users.clear();
		users = null;
		groups.clear();
		groups = null;
	}

	protected DirectoryUser daoGetRole(LdapName key) {
		if (groups.containsKey(key))
			return groups.get(key);
		if (users.containsKey(key))
			return users.get(key);
		return null;
	}

	protected Boolean daoHasRole(LdapName dn) {
		return users.containsKey(dn) || groups.containsKey(dn);
	}

	@SuppressWarnings("unchecked")
	protected List<DirectoryUser> doGetRoles(Filter f) {
		ArrayList<DirectoryUser> res = new ArrayList<DirectoryUser>();
		if (f == null) {
			res.addAll(users.values());
			res.addAll(groups.values());
		} else {
			for (DirectoryUser user : users.values()) {
				// System.out.println("\n" + user.getName());
				// Dictionary<String, Object> props = user.getProperties();
				// for (Enumeration<String> keys = props.keys(); keys
				// .hasMoreElements();) {
				// String key = keys.nextElement();
				// System.out.println(" " + key + "=" + props.get(key));
				// }
				if (f.match(user.getProperties()))
					res.add(user);
			}
			for (DirectoryUser group : groups.values())
				if (f.match(group.getProperties()))
					res.add(group);
		}
		return res;
	}

	@Override
	protected List<LdapName> getDirectGroups(LdapName dn) {
		List<LdapName> directGroups = new ArrayList<LdapName>();
		for (LdapName name : groups.keySet()) {
			DirectoryGroup group = groups.get(name);
			if (group.getMemberNames().contains(dn))
				directGroups.add(group.getDn());
		}
		return directGroups;
	}

	@Override
	protected void prepare(UserDirectoryWorkingCopy wc) {
		// delete
		for (LdapName dn : wc.getDeletedUsers().keySet()) {
			if (users.containsKey(dn))
				users.remove(dn);
			else if (groups.containsKey(dn))
				groups.remove(dn);
			else
				throw new UserDirectoryException("User to delete not found " + dn);
		}
		// add
		for (LdapName dn : wc.getNewUsers().keySet()) {
			DirectoryUser user = wc.getNewUsers().get(dn);
			if (users.containsKey(dn) || groups.containsKey(dn))
				throw new UserDirectoryException("User to create found " + dn);
			else if (Role.USER == user.getType())
				users.put(dn, user);
			else if (Role.GROUP == user.getType())
				groups.put(dn, (DirectoryGroup) user);
			else
				throw new UserDirectoryException("Unsupported role type " + user.getType() + " for new user " + dn);
		}
		// modify
		for (LdapName dn : wc.getModifiedUsers().keySet()) {
			Attributes modifiedAttrs = wc.getModifiedUsers().get(dn);
			DirectoryUser user;
			if (users.containsKey(dn))
				user = users.get(dn);
			else if (groups.containsKey(dn))
				user = groups.get(dn);
			else
				throw new UserDirectoryException("User to modify no found " + dn);
			user.publishAttributes(modifiedAttrs);
		}
	}

	@Override
	protected void commit(UserDirectoryWorkingCopy wc) {
		save();
	}

	@Override
	protected void rollback(UserDirectoryWorkingCopy wc) {
		init();
	}

}
