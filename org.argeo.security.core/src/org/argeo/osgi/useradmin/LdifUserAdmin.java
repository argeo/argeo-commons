package org.argeo.osgi.useradmin;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.naming.NamingEnumeration;
import javax.naming.directory.Attributes;
import javax.naming.ldap.LdapName;
import javax.transaction.TransactionManager;

import org.apache.commons.io.IOUtils;
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
		setReadOnly(true);
		setUri(null);
	}

	private static Dictionary<String, Object> fromUri(String uri, String baseDn) {
		Hashtable<String, Object> res = new Hashtable<String, Object>();
		res.put(UserAdminConf.uri.property(), uri);
		res.put(UserAdminConf.baseDn.property(), baseDn);
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
			users.clear();
			groups.clear();

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
			// Filter f = FrameworkUtil.createFilter(filter);
			for (DirectoryUser user : users.values())
				if (f.match(user.getProperties()))
					res.add(user);
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
	protected void prepare(WorkingCopy wc) {
		// delete
		for (LdapName dn : wc.getDeletedUsers().keySet()) {
			if (users.containsKey(dn))
				users.remove(dn);
			else if (groups.containsKey(dn))
				groups.remove(dn);
			else
				throw new UserDirectoryException("User to delete no found "
						+ dn);
		}
		// add
		for (LdapName dn : wc.getNewUsers().keySet()) {
			DirectoryUser user = wc.getNewUsers().get(dn);
			if (Role.USER == user.getType())
				users.put(dn, user);
			else if (Role.GROUP == user.getType())
				groups.put(dn, (DirectoryGroup) user);
			else
				throw new UserDirectoryException("Unsupported role type "
						+ user.getType() + " for new user " + dn);
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
				throw new UserDirectoryException("User to modify no found "
						+ dn);
			user.publishAttributes(modifiedAttrs);
		}
	}

	@Override
	protected void commit(WorkingCopy wc) {
		save();
	}

	@Override
	protected void rollback(WorkingCopy wc) {
		init();
	}

}
