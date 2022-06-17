package org.argeo.osgi.useradmin;

import static org.argeo.util.naming.LdapAttrs.objectClass;
import static org.argeo.util.naming.LdapObjs.inetOrgPerson;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.naming.NameNotFoundException;
import javax.naming.NamingEnumeration;
import javax.naming.directory.Attributes;
import javax.naming.ldap.LdapName;

import org.argeo.util.naming.LdapObjs;
import org.argeo.util.naming.LdifParser;
import org.argeo.util.naming.LdifWriter;
import org.osgi.framework.Filter;
import org.osgi.service.useradmin.Role;
import org.osgi.service.useradmin.User;

/** A user admin based on a LDIF files. */
public class LdifUserAdmin extends AbstractUserDirectory {
	private SortedMap<LdapName, DirectoryUser> users = new TreeMap<>();
	private SortedMap<LdapName, DirectoryGroup> groups = new TreeMap<>();

	private SortedMap<LdapName, LdifHierarchyUnit> hierarchy = new TreeMap<>();
	private List<HierarchyUnit> rootHierarchyUnits = new ArrayList<>();

	public LdifUserAdmin(String uri, String baseDn) {
		this(fromUri(uri, baseDn), false);
	}

	public LdifUserAdmin(Dictionary<String, ?> properties) {
		this(properties, false);
	}

	protected LdifUserAdmin(Dictionary<String, ?> properties, boolean scoped) {
		super(null, properties, scoped);
	}

	public LdifUserAdmin(URI uri, Dictionary<String, ?> properties) {
		super(uri, properties, false);
	}

	@Override
	protected AbstractUserDirectory scope(User user) {
		Dictionary<String, Object> credentials = user.getCredentials();
		String username = (String) credentials.get(SHARED_STATE_USERNAME);
		if (username == null)
			username = user.getName();
		Object pwdCred = credentials.get(SHARED_STATE_PASSWORD);
		byte[] pwd = (byte[]) pwdCred;
		if (pwd != null) {
			char[] password = DigestUtils.bytesToChars(pwd);
			User directoryUser = (User) getRole(username);
			if (!directoryUser.hasCredential(null, password))
				throw new UserDirectoryException("Invalid credentials");
		} else {
			throw new UserDirectoryException("Password is required");
		}
		Dictionary<String, Object> properties = cloneProperties();
		properties.put(UserAdminConf.readOnly.name(), "true");
		LdifUserAdmin scopedUserAdmin = new LdifUserAdmin(properties, true);
		scopedUserAdmin.groups = Collections.unmodifiableSortedMap(groups);
		scopedUserAdmin.users = Collections.unmodifiableSortedMap(users);
		return scopedUserAdmin;
	}

	private static Dictionary<String, Object> fromUri(String uri, String baseDn) {
		Hashtable<String, Object> res = new Hashtable<String, Object>();
		res.put(UserAdminConf.uri.name(), uri);
		res.put(UserAdminConf.baseDn.name(), baseDn);
		return res;
	}

	public void init() {

		try {
			URI u = new URI(getUri());
			if (u.getScheme().equals("file")) {
				File file = new File(u);
				if (!file.exists())
					return;
			}
			load(u.toURL().openStream());
		} catch (Exception e) {
			throw new UserDirectoryException("Cannot open URL " + getUri(), e);
		}
	}

	public void save() {
		if (getUri() == null)
			throw new UserDirectoryException("Cannot save LDIF user admin: no URI is set");
		if (isReadOnly())
			throw new UserDirectoryException("Cannot save LDIF user admin: " + getUri() + " is read-only");
		try (FileOutputStream out = new FileOutputStream(new File(new URI(getUri())))) {
			save(out);
		} catch (IOException | URISyntaxException e) {
			throw new UserDirectoryException("Cannot save user admin to " + getUri(), e);
		}
	}

	public void save(OutputStream out) throws IOException {
		try {
			LdifWriter ldifWriter = new LdifWriter(out);
			for (LdapName name : hierarchy.keySet())
				ldifWriter.writeEntry(name, hierarchy.get(name).getAttributes());
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
			hierarchy.clear();

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
					if (objectClass.toLowerCase().equals(inetOrgPerson.name().toLowerCase())) {
						users.put(key, new LdifUser(this, key, attributes));
						break objectClasses;
					} else if (objectClass.toLowerCase().equals(getGroupObjectClass().toLowerCase())) {
						groups.put(key, new LdifGroup(this, key, attributes));
						break objectClasses;
					} else if (objectClass.equalsIgnoreCase(LdapObjs.organization.name())) {
						// we only consider organizations which are not groups
						hierarchy.put(key, new LdifHierarchyUnit(this, key, HierarchyUnit.ORGANIZATION, attributes));
						break objectClasses;
					} else if (objectClass.equalsIgnoreCase(LdapObjs.organizationalUnit.name())) {
						String name = key.getRdn(key.size() - 1).toString();
						if (getUserBase().equalsIgnoreCase(name) || getGroupBase().equalsIgnoreCase(name))
							break objectClasses; // skip
						// TODO skip if it does not contain groups or users
						hierarchy.put(key, new LdifHierarchyUnit(this, key, HierarchyUnit.OU, attributes));
						break objectClasses;
					}
				}
			}

			// link hierarchy
			hierachyUnits: for (LdapName dn : hierarchy.keySet()) {
				LdifHierarchyUnit unit = hierarchy.get(dn);
				LdapName parentDn = (LdapName) dn.getPrefix(dn.size() - 1);
				LdifHierarchyUnit parent = hierarchy.get(parentDn);
				if (parent == null) {
					rootHierarchyUnits.add(unit);
					unit.parent = this;
					continue hierachyUnits;
				}
				parent.children.add(unit);
				unit.parent = parent;
			}
		} catch (Exception e) {
			throw new UserDirectoryException("Cannot load user admin service from LDIF", e);
		}
	}

	public void destroy() {
		if (users == null || groups == null)
			throw new UserDirectoryException("User directory " + getBaseDn() + " is already destroyed");
		users = null;
		groups = null;
	}

	/*
	 * USER ADMIN
	 */

	@Override
	protected DirectoryUser daoGetRole(LdapName key) throws NameNotFoundException {
		if (groups.containsKey(key))
			return groups.get(key);
		if (users.containsKey(key))
			return users.get(key);
		throw new NameNotFoundException(key + " not persisted");
	}

	@Override
	protected Boolean daoHasRole(LdapName dn) {
		return users.containsKey(dn) || groups.containsKey(dn);
	}

	@Override
	protected List<DirectoryUser> doGetRoles(LdapName searchBase, Filter f, boolean deep) {
		Objects.requireNonNull(searchBase);
		ArrayList<DirectoryUser> res = new ArrayList<DirectoryUser>();
		if (f == null && deep && getBaseDn().equals(searchBase)) {
			res.addAll(users.values());
			res.addAll(groups.values());
		} else {
			filterRoles(users, searchBase, f, deep, res);
			filterRoles(groups, searchBase, f, deep, res);
		}
		return res;
	}

	private void filterRoles(SortedMap<LdapName, ? extends DirectoryUser> map, LdapName searchBase, Filter f,
			boolean deep, List<DirectoryUser> res) {
		// TODO reduce map with search base ?
		roles: for (DirectoryUser user : map.values()) {
			LdapName dn = user.getDn();
			if (dn.startsWith(searchBase)) {
				if (!deep && dn.size() != (searchBase.size() + 1))
					continue roles;
				if (f == null)
					res.add(user);
				else if (f.match(user.getProperties()))
					res.add(user);
			}
		}

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

	/*
	 * HIERARCHY
	 */
	@Override
	public int getHierarchyChildCount() {
		return rootHierarchyUnits.size();
	}

	@Override
	public HierarchyUnit getHierarchyChild(int i) {
		return rootHierarchyUnits.get(i);
	}

	@Override
	public HierarchyUnit getHierarchyUnit(String path) {
		LdapName dn = LdapNameUtils.toLdapName(path);
		return hierarchy.get(dn);
	}

	@Override
	public HierarchyUnit getHierarchyUnit(Role role) {
		LdapName dn = LdapNameUtils.toLdapName(role.getName());
		// 2 levels
		LdapName huDn = LdapNameUtils.getParent(LdapNameUtils.getParent(dn));
		HierarchyUnit hierarchyUnit = hierarchy.get(huDn);
		if (hierarchyUnit == null)
			throw new IllegalStateException("No hierarchy unit found for " + role);
		return hierarchyUnit;
	}

}
