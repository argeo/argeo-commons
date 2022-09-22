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
import java.util.HashSet;
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

import org.argeo.util.directory.HierarchyUnit;
import org.argeo.util.naming.LdapObjs;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.useradmin.Role;

/** A user admin based on a LDIF files. */
public class LdifDao extends AbstractLdapDirectoryDao {
	private NavigableMap<LdapName, LdapEntry> entries = new TreeMap<>();
	private NavigableMap<LdapName, LdapHierarchyUnit> hierarchy = new TreeMap<>();

	private NavigableMap<LdapName, Attributes> values = new TreeMap<>();

	public LdifDao(AbstractLdapDirectory directory) {
		super(directory);
	}

	public void init() {
		String uri = getDirectory().getUri();
		if (uri == null)
			return;
		try {
			URI u = new URI(uri);
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
			return; // ignore
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
			for (LdapName name : entries.keySet())
				ldifWriter.writeEntry(name, entries.get(name).getAttributes());
		} finally {
			out.close();
		}
	}

	public void load(InputStream in) {
		try {
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

				values.put(key, attributes);

				// analyse object classes
				NamingEnumeration<?> objectClasses = attributes.get(objectClass.name()).getAll();
				// System.out.println(key);
				objectClasses: while (objectClasses.hasMore()) {
					String objectClass = objectClasses.next().toString();
					// System.out.println(" " + objectClass);
					if (objectClass.toLowerCase().equals(inetOrgPerson.name().toLowerCase())) {
						entries.put(key, newUser(key));
						break objectClasses;
					} else if (objectClass.toLowerCase().equals(getDirectory().getGroupObjectClass().toLowerCase())) {
						entries.put(key, newGroup(key));
						break objectClasses;
					} else if (objectClass.equalsIgnoreCase(LdapObjs.organizationalUnit.name())) {
						// TODO skip if it does not contain groups or users
						hierarchy.put(key, new LdapHierarchyUnit(getDirectory(), key));
						break objectClasses;
					}
				}
			}

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
	public LdapEntry doGetEntry(LdapName key) throws NameNotFoundException {
		if (entries.containsKey(key))
			return entries.get(key);
		throw new NameNotFoundException(key + " not persisted");
	}

	@Override
	public Attributes doGetAttributes(LdapName name) {
		if (!values.containsKey(name))
			throw new IllegalStateException(name + " doe not exist in " + getDirectory().getBaseDn());
		return values.get(name);
	}

	@Override
	public boolean checkConnection() {
		return true;
	}

	@Override
	public boolean entryExists(LdapName dn) {
		return entries.containsKey(dn);// || groups.containsKey(dn);
	}

	@Override
	public List<LdapEntry> doGetEntries(LdapName searchBase, String f, boolean deep) {
		Objects.requireNonNull(searchBase);
		ArrayList<LdapEntry> res = new ArrayList<>();
		if (f == null && deep && getDirectory().getBaseDn().equals(searchBase)) {
			res.addAll(entries.values());
		} else {
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
				LdapEntry entry = doGetEntry(name);
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
			else
				throw new IllegalStateException("User to delete not found " + dn);
		}
		// add
		for (LdapName dn : wc.getNewData().keySet()) {
			LdapEntry user = (LdapEntry) wc.getNewData().get(dn);
			if (entries.containsKey(dn))
				throw new IllegalStateException("User to create found " + dn);
			entries.put(dn, user);
		}
		// modify
		for (LdapName dn : wc.getModifiedData().keySet()) {
			Attributes modifiedAttrs = wc.getModifiedData().get(dn);
			LdapEntry user;
			try {
				user = doGetEntry(dn);
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
	@Override
	public HierarchyUnit doGetHierarchyUnit(LdapName dn) {
		if (getDirectory().getBaseDn().equals(dn))
			return getDirectory();
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
}
