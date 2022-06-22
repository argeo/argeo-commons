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
import java.util.NavigableMap;
import java.util.Objects;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.naming.NameNotFoundException;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.ldap.LdapName;

import org.argeo.util.directory.DirectoryConf;
import org.argeo.util.directory.DirectoryDigestUtils;
import org.argeo.util.directory.HierarchyUnit;
import org.argeo.util.directory.ldap.LdapEntry;
import org.argeo.util.directory.ldap.LdapEntryWorkingCopy;
import org.argeo.util.directory.ldap.LdapHierarchyUnit;
import org.argeo.util.directory.ldap.LdifParser;
import org.argeo.util.directory.ldap.LdifWriter;
import org.argeo.util.naming.LdapObjs;
import org.osgi.framework.Filter;
import org.osgi.service.useradmin.Role;
import org.osgi.service.useradmin.User;

/** A user admin based on a LDIF files. */
public class LdifUserAdmin extends AbstractUserDirectory {
	private NavigableMap<LdapName, LdapEntry> users = new TreeMap<>();
	private NavigableMap<LdapName, LdapEntry> groups = new TreeMap<>();

	private NavigableMap<LdapName, LdapHierarchyUnit> hierarchy = new TreeMap<>();
//	private List<HierarchyUnit> rootHierarchyUnits = new ArrayList<>();

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
			char[] password = DirectoryDigestUtils.bytesToChars(pwd);
			User directoryUser = (User) getRole(username);
			if (!directoryUser.hasCredential(null, password))
				throw new IllegalStateException("Invalid credentials");
		} else {
			throw new IllegalStateException("Password is required");
		}
		Dictionary<String, Object> properties = cloneProperties();
		properties.put(DirectoryConf.readOnly.name(), "true");
		LdifUserAdmin scopedUserAdmin = new LdifUserAdmin(properties, true);
		scopedUserAdmin.groups = Collections.unmodifiableNavigableMap(groups);
		scopedUserAdmin.users = Collections.unmodifiableNavigableMap(users);
		return scopedUserAdmin;
	}

	private static Dictionary<String, Object> fromUri(String uri, String baseDn) {
		Hashtable<String, Object> res = new Hashtable<String, Object>();
		res.put(DirectoryConf.uri.name(), uri);
		res.put(DirectoryConf.baseDn.name(), baseDn);
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
		} catch (IOException | URISyntaxException e) {
			throw new IllegalStateException("Cannot open URL " + getUri(), e);
		}
	}

	public void save() {
		if (getUri() == null)
			throw new IllegalStateException("Cannot save LDIF user admin: no URI is set");
		if (isReadOnly())
			throw new IllegalStateException("Cannot save LDIF user admin: " + getUri() + " is read-only");
		try (FileOutputStream out = new FileOutputStream(new File(new URI(getUri())))) {
			save(out);
		} catch (IOException | URISyntaxException e) {
			throw new IllegalStateException("Cannot save user admin to " + getUri(), e);
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
						throw new IllegalStateException(key + " has duplicate id " + id);
					lowerCase.add(id);
				}

				// analyse object classes
				NamingEnumeration<?> objectClasses = attributes.get(objectClass.name()).getAll();
				// System.out.println(key);
				objectClasses: while (objectClasses.hasMore()) {
					String objectClass = objectClasses.next().toString();
					// System.out.println(" " + objectClass);
					if (objectClass.toLowerCase().equals(inetOrgPerson.name().toLowerCase())) {
						users.put(key, newUser(key, attributes));
						break objectClasses;
					} else if (objectClass.toLowerCase().equals(getGroupObjectClass().toLowerCase())) {
						groups.put(key, newGroup(key, attributes));
						break objectClasses;
//					} else if (objectClass.equalsIgnoreCase(LdapObjs.organization.name())) {
//						// we only consider organizations which are not groups
//						hierarchy.put(key, new LdifHierarchyUnit(this, key, HierarchyUnit.ORGANIZATION, attributes));
//						break objectClasses;
					} else if (objectClass.equalsIgnoreCase(LdapObjs.organizationalUnit.name())) {
//						String name = key.getRdn(key.size() - 1).toString();
//						if (getUserBase().equalsIgnoreCase(name) || getGroupBase().equalsIgnoreCase(name))
//							break objectClasses; // skip
						// TODO skip if it does not contain groups or users
						hierarchy.put(key, new LdapHierarchyUnit(this, key, attributes));
						break objectClasses;
					}
				}
			}

			// link hierarchy
//			hierachyUnits: for (LdapName dn : hierarchy.keySet()) {
//				LdifHierarchyUnit unit = hierarchy.get(dn);
//				LdapName parentDn = (LdapName) dn.getPrefix(dn.size() - 1);
//				LdifHierarchyUnit parent = hierarchy.get(parentDn);
//				if (parent == null) {
//					rootHierarchyUnits.add(unit);
//					unit.parent = null;
//					continue hierachyUnits;
//				}
//				parent.children.add(unit);
//				unit.parent = parent;
//			}
		} catch (NamingException | IOException e) {
			throw new IllegalStateException("Cannot load user admin service from LDIF", e);
		}
	}

	public void destroy() {
		if (users == null || groups == null)
			throw new IllegalStateException("User directory " + getBaseDn() + " is already destroyed");
		users = null;
		groups = null;
	}

	/*
	 * USER ADMIN
	 */

	@Override
	protected DirectoryUser daoGetEntry(LdapName key) throws NameNotFoundException {
		if (groups.containsKey(key))
			return (DirectoryUser) groups.get(key);
		if (users.containsKey(key))
			return (DirectoryUser) users.get(key);
		throw new NameNotFoundException(key + " not persisted");
	}

	@Override
	protected Boolean daoHasEntry(LdapName dn) {
		return users.containsKey(dn) || groups.containsKey(dn);
	}

	@Override
	protected List<LdapEntry> doGetEntries(LdapName searchBase, Filter f, boolean deep) {
		Objects.requireNonNull(searchBase);
		ArrayList<LdapEntry> res = new ArrayList<>();
		if (f == null && deep && getBaseDn().equals(searchBase)) {
			res.addAll(users.values());
			res.addAll(groups.values());
		} else {
			filterRoles(users, searchBase, f, deep, res);
			filterRoles(groups, searchBase, f, deep, res);
		}
		return res;
	}

	private void filterRoles(SortedMap<LdapName, ? extends LdapEntry> map, LdapName searchBase, Filter f, boolean deep,
			List<LdapEntry> res) {
		// TODO reduce map with search base ?
		roles: for (LdapEntry user : map.values()) {
			LdapName dn = user.getDn();
			if (dn.startsWith(searchBase)) {
				if (!deep && dn.size() != (searchBase.size() + 1))
					continue roles;
				if (f == null)
					res.add(user);
				else {
					if (f.match(((DirectoryUser) user).getProperties()))
						res.add(user);
				}
			}
		}

	}

	@Override
	protected List<LdapName> getDirectGroups(LdapName dn) {
		List<LdapName> directGroups = new ArrayList<LdapName>();
		for (LdapName name : groups.keySet()) {
			DirectoryGroup group;
			try {
				group = (DirectoryGroup) daoGetEntry(name);
			} catch (NameNotFoundException e) {
				throw new IllegalArgumentException("Group " + dn + " not found", e);
			}
			if (group.getMemberNames().contains(dn))
				directGroups.add(group.getDn());
		}
		return directGroups;
	}

	@Override
	public void prepare(LdapEntryWorkingCopy wc) {
		// delete
		for (LdapName dn : wc.getDeletedData().keySet()) {
			if (users.containsKey(dn))
				users.remove(dn);
			else if (groups.containsKey(dn))
				groups.remove(dn);
			else
				throw new IllegalStateException("User to delete not found " + dn);
		}
		// add
		for (LdapName dn : wc.getNewData().keySet()) {
			DirectoryUser user = (DirectoryUser) wc.getNewData().get(dn);
			if (users.containsKey(dn) || groups.containsKey(dn))
				throw new IllegalStateException("User to create found " + dn);
			else if (Role.USER == user.getType())
				users.put(dn, user);
			else if (Role.GROUP == user.getType())
				groups.put(dn, (DirectoryGroup) user);
			else
				throw new IllegalStateException("Unsupported role type " + user.getType() + " for new user " + dn);
		}
		// modify
		for (LdapName dn : wc.getModifiedData().keySet()) {
			Attributes modifiedAttrs = wc.getModifiedData().get(dn);
			DirectoryUser user;
			try {
				user = daoGetEntry(dn);
			} catch (NameNotFoundException e) {
				throw new IllegalStateException("User to modify no found " + dn, e);
			}
			if (user == null)
				throw new IllegalStateException("User to modify no found " + dn);
			user.publishAttributes(modifiedAttrs);
		}
	}

	@Override
	public void commit(LdapEntryWorkingCopy wc) {
		save();
	}

	@Override
	public void rollback(LdapEntryWorkingCopy wc) {
		init();
	}

	/*
	 * HIERARCHY
	 */

//	@Override
//	public int getHierarchyChildCount() {
//		return rootHierarchyUnits.size();
//	}
//
//	@Override
//	public HierarchyUnit getHierarchyChild(int i) {
//		return rootHierarchyUnits.get(i);
//	}
	@Override
	public HierarchyUnit doGetHierarchyUnit(LdapName dn) {
		return hierarchy.get(dn);
	}

	@Override
	public Iterable<HierarchyUnit> doGetDirectHierarchyUnits(LdapName searchBase, boolean functionalOnly) {
		List<HierarchyUnit> res = new ArrayList<>();
		for (LdapName n : hierarchy.keySet()) {
			if (n.size() == searchBase.size() + 1) {
				if (n.startsWith(searchBase)) {
					HierarchyUnit hu = hierarchy.get(n);
					if (functionalOnly) {
						if (hu.isFunctional())
							res.add(hu);
					} else {
						res.add(hu);
					}
				}
			}
		}
		return res;
	}

//	@Override
//	public Iterable<HierarchyUnit> getDirectHierarchyUnits(boolean functionalOnly) {
//		if (functionalOnly) {
//			List<HierarchyUnit> res = new ArrayList<>();
//			for (HierarchyUnit hu : rootHierarchyUnits) {
//				if (hu.isFunctional())
//					res.add(hu);
//			}
//			return res;
//
//		} else {
//			return rootHierarchyUnits;
//		}
//	}

}
