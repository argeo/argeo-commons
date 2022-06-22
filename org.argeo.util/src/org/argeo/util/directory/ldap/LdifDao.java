package org.argeo.util.directory.ldap;

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
import org.argeo.util.directory.HierarchyUnit;
import org.argeo.util.naming.LdapObjs;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.useradmin.Role;

/** A user admin based on a LDIF files. */
public class LdifDao extends AbstractLdapDirectoryDao {
//	private NavigableMap<LdapName, LdapEntry> users = new TreeMap<>();
//	private NavigableMap<LdapName, LdapEntry> groups = new TreeMap<>();
	private NavigableMap<LdapName, LdapEntry> entries = new TreeMap<>();

	private NavigableMap<LdapName, LdapHierarchyUnit> hierarchy = new TreeMap<>();
//	private List<HierarchyUnit> rootHierarchyUnits = new ArrayList<>();

//	public LdifUserAdmin(String uri, String baseDn) {
//		this(fromUri(uri, baseDn), false);
//	}

	public LdifDao(AbstractLdapDirectory directory) {
		super(directory);
	}

//	protected LdifUserAdmin(Hashtable<String, ?> properties, boolean scoped) {
//		super( properties, scoped);
//	}

//	public LdifUserAdmin(URI uri, Dictionary<String, ?> properties) {
//		super(uri, properties, false);
//	}

//	@Override
//	protected AbstractUserDirectory scope(User user) {
//		Dictionary<String, Object> credentials = user.getCredentials();
//		String username = (String) credentials.get(SHARED_STATE_USERNAME);
//		if (username == null)
//			username = user.getName();
//		Object pwdCred = credentials.get(SHARED_STATE_PASSWORD);
//		byte[] pwd = (byte[]) pwdCred;
//		if (pwd != null) {
//			char[] password = DirectoryDigestUtils.bytesToChars(pwd);
//			User directoryUser = (User) getRole(username);
//			if (!directoryUser.hasCredential(null, password))
//				throw new IllegalStateException("Invalid credentials");
//		} else {
//			throw new IllegalStateException("Password is required");
//		}
//		Dictionary<String, Object> properties = cloneProperties();
//		properties.put(DirectoryConf.readOnly.name(), "true");
//		LdifUserAdmin scopedUserAdmin = new LdifUserAdmin(properties, true);
////		scopedUserAdmin.groups = Collections.unmodifiableNavigableMap(groups);
////		scopedUserAdmin.users = Collections.unmodifiableNavigableMap(users);
//		scopedUserAdmin.entries = Collections.unmodifiableNavigableMap(entries);
//		return scopedUserAdmin;
//	}

	private static Dictionary<String, Object> fromUri(String uri, String baseDn) {
		Hashtable<String, Object> res = new Hashtable<String, Object>();
		res.put(DirectoryConf.uri.name(), uri);
		res.put(DirectoryConf.baseDn.name(), baseDn);
		return res;
	}

	public void init() {

		try {
			URI u = new URI(getDirectory().getUri());
			if (u.getScheme().equals("file")) {
				File file = new File(u);
				if (!file.exists())
					return;
			}
			load(u.toURL().openStream());
		} catch (IOException | URISyntaxException e) {
			throw new IllegalStateException("Cannot open URL " + getDirectory().getUri(), e);
		}
	}

	public void save() {
		if (getDirectory().getUri() == null)
			throw new IllegalStateException("Cannot save LDIF user admin: no URI is set");
		if (getDirectory().isReadOnly())
			throw new IllegalStateException(
					"Cannot save LDIF user admin: " + getDirectory().getUri() + " is read-only");
		try (FileOutputStream out = new FileOutputStream(new File(new URI(getDirectory().getUri())))) {
			save(out);
		} catch (IOException | URISyntaxException e) {
			throw new IllegalStateException("Cannot save user admin to " + getDirectory().getUri(), e);
		}
	}

	public void save(OutputStream out) throws IOException {
		try {
			LdifWriter ldifWriter = new LdifWriter(out);
			for (LdapName name : hierarchy.keySet())
				ldifWriter.writeEntry(name, hierarchy.get(name).getAttributes());
//			for (LdapName name : groups.keySet())
//				ldifWriter.writeEntry(name, groups.get(name).getAttributes());
//			for (LdapName name : users.keySet())
//				ldifWriter.writeEntry(name, users.get(name).getAttributes());
			for (LdapName name : entries.keySet())
				ldifWriter.writeEntry(name, entries.get(name).getAttributes());
		} finally {
			out.close();
		}
	}

	public void load(InputStream in) {
		try {
//			users.clear();
//			groups.clear();
			entries.clear();
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
						entries.put(key, newUser(key, attributes));
						break objectClasses;
					} else if (objectClass.toLowerCase().equals(getDirectory().getGroupObjectClass().toLowerCase())) {
						entries.put(key, newGroup(key, attributes));
						break objectClasses;
//					} else if (objectClass.equalsIgnoreCase(LdapObjs.organization.name())) {
//						// we only consider organizations which are not groups
//						hierarchy.put(key, new LdifHierarchyUnit(this, key, HierarchyUnit.ORGANIZATION, attributes));
//						break objectClasses;
					} else if (objectClass.equalsIgnoreCase(LdapObjs.organizationalUnit.name())) {
//						String name = key.getRdn(key.size() - 1).toStrindirectoryDaog();
//						if (getUserBase().equalsIgnoreCase(name) || getGroupBase().equalsIgnoreCase(name))
//							break objectClasses; // skip
						// TODO skip if it does not contain groups or users
						hierarchy.put(key, new LdapHierarchyUnit(getDirectory(), key, attributes));
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
//		if (users == null || groups == null)
		if (entries == null)
			throw new IllegalStateException("User directory " + getDirectory().getBaseDn() + " is already destroyed");
//		users = null;
//		groups = null;
		entries = null;
	}

	/*
	 * USER ADMIN
	 */

	@Override
	public LdapEntry daoGetEntry(LdapName key) throws NameNotFoundException {
//		if (groups.containsKey(key))
//			return groups.get(key);
//		if (users.containsKey(key))
//			return users.get(key);
		if (entries.containsKey(key))
			return entries.get(key);
		throw new NameNotFoundException(key + " not persisted");
	}

	@Override
	public Boolean daoHasEntry(LdapName dn) {
		return entries.containsKey(dn);// || groups.containsKey(dn);
	}

	@Override
	public List<LdapEntry> doGetEntries(LdapName searchBase, String f, boolean deep) {
		Objects.requireNonNull(searchBase);
		ArrayList<LdapEntry> res = new ArrayList<>();
		if (f == null && deep && getDirectory().getBaseDn().equals(searchBase)) {
//			res.addAll(users.values());
//			res.addAll(groups.values());
			res.addAll(entries.values());
		} else {
//			filterRoles(users, searchBase, f, deep, res);
//			filterRoles(groups, searchBase, f, deep, res);
			filterRoles(entries, searchBase, f, deep, res);
		}
		return res;
	}

	private void filterRoles(SortedMap<LdapName, ? extends LdapEntry> map, LdapName searchBase, String f, boolean deep,
			List<LdapEntry> res) {
		// FIXME get rid of OSGi references
		try {
			// TODO reduce map with search base ?
			Filter filter = f != null ? FrameworkUtil.createFilter(f) : null;
			roles: for (LdapEntry user : map.values()) {
				LdapName dn = user.getDn();
				if (dn.startsWith(searchBase)) {
					if (!deep && dn.size() != (searchBase.size() + 1))
						continue roles;
					if (filter == null)
						res.add(user);
					else {
						if (user instanceof Role) {
							if (filter.match(((Role) user).getProperties()))
								res.add(user);
						}
					}
				}
			}
		} catch (InvalidSyntaxException e) {
			throw new IllegalArgumentException("Cannot create filter " + f, e);
		}

	}

	@Override
	public List<LdapName> getDirectGroups(LdapName dn) {
		List<LdapName> directGroups = new ArrayList<LdapName>();
		entries: for (LdapName name : entries.keySet()) {
			LdapEntry group;
			try {
				LdapEntry entry = daoGetEntry(name);
				if (AbstractLdapDirectory.hasObjectClass(entry.getAttributes(), getDirectory().getGroupObjectClass())) {
					group = entry;
				} else {
					continue entries;
				}
			} catch (NameNotFoundException e) {
				throw new IllegalArgumentException("Group " + dn + " not found", e);
			}
			if (group.getReferences(getDirectory().getMemberAttributeId()).contains(dn)) {
				directGroups.add(group.getDn());
			}
		}
		return directGroups;
	}

	@Override
	public void prepare(LdapEntryWorkingCopy wc) {
		// delete
		for (LdapName dn : wc.getDeletedData().keySet()) {
			if (entries.containsKey(dn))
				entries.remove(dn);
//			if (users.containsKey(dn))
//				users.remove(dn);
//			else if (groups.containsKey(dn))
//				groups.remove(dn);
			else
				throw new IllegalStateException("User to delete not found " + dn);
		}
		// add
		for (LdapName dn : wc.getNewData().keySet()) {
			LdapEntry user = (LdapEntry) wc.getNewData().get(dn);
//			if (users.containsKey(dn) || groups.containsKey(dn))
			if (entries.containsKey(dn))
				throw new IllegalStateException("User to create found " + dn);
			entries.put(dn, user);
//			else if (Role.USER == user.getType())
//				users.put(dn, user);
//			else if (Role.GROUP == user.getType())
//				groups.put(dn, (DirectoryGroup) user);
//			else
//				throw new IllegalStateException("Unsupported role type " + user.getType() + " for new user " + dn);
		}
		// modify
		for (LdapName dn : wc.getModifiedData().keySet()) {
			Attributes modifiedAttrs = wc.getModifiedData().get(dn);
			LdapEntry user;
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

	public void scope(LdifDao scoped) {
		scoped.entries = Collections.unmodifiableNavigableMap(entries);
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
