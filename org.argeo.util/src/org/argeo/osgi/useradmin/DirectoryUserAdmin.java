package org.argeo.osgi.useradmin;

import static org.argeo.util.naming.LdapAttrs.objectClass;
import static org.argeo.util.naming.LdapObjs.extensibleObject;
import static org.argeo.util.naming.LdapObjs.inetOrgPerson;
import static org.argeo.util.naming.LdapObjs.organizationalPerson;
import static org.argeo.util.naming.LdapObjs.person;
import static org.argeo.util.naming.LdapObjs.top;

import java.net.URI;
import java.nio.channels.UnsupportedAddressTypeException;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Iterator;
import java.util.List;

import javax.naming.Context;
import javax.naming.InvalidNameException;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;

import org.argeo.util.directory.DirectoryConf;
import org.argeo.util.directory.DirectoryDigestUtils;
import org.argeo.util.directory.HierarchyUnit;
import org.argeo.util.directory.ldap.AbstractLdapDirectory;
import org.argeo.util.directory.ldap.LdapEntry;
import org.argeo.util.directory.ldap.LdapEntryWorkingCopy;
import org.argeo.util.directory.ldap.LdapNameUtils;
import org.argeo.util.directory.ldap.LdifDao;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.useradmin.Authorization;
import org.osgi.service.useradmin.Role;
import org.osgi.service.useradmin.User;
import org.osgi.service.useradmin.UserAdmin;

/** Base class for a {@link UserDirectory}. */
public class DirectoryUserAdmin extends AbstractLdapDirectory implements UserAdmin, UserDirectory {

	private UserAdmin externalRoles;
	// private List<String> indexedUserProperties = Arrays
	// .asList(new String[] { LdapAttrs.uid.name(), LdapAttrs.mail.name(),
	// LdapAttrs.cn.name() });

	// Transaction
//	private TransactionManager transactionManager;
	public DirectoryUserAdmin(URI uriArg, Dictionary<String, ?> props) {
		this(uriArg, props, false);
	}

	public DirectoryUserAdmin(URI uriArg, Dictionary<String, ?> props, boolean scoped) {
		super(uriArg, props, scoped);
	}

	public DirectoryUserAdmin(Dictionary<String, ?> props) {
		this(null, props);
	}

	/*
	 * ABSTRACT METHODS
	 */

	protected AbstractLdapDirectory scope(User user) {
		throw new UnsupportedAddressTypeException();
	}

	protected DirectoryUserAdmin scopeLdap(User user) {
		Dictionary<String, Object> credentials = user.getCredentials();
		String username = (String) credentials.get(SHARED_STATE_USERNAME);
		if (username == null)
			username = user.getName();
		Dictionary<String, Object> properties = cloneConfigProperties();
		properties.put(Context.SECURITY_PRINCIPAL, username.toString());
		Object pwdCred = credentials.get(SHARED_STATE_PASSWORD);
		byte[] pwd = (byte[]) pwdCred;
		if (pwd != null) {
			char[] password = DirectoryDigestUtils.bytesToChars(pwd);
			properties.put(Context.SECURITY_CREDENTIALS, new String(password));
		} else {
			properties.put(Context.SECURITY_AUTHENTICATION, "GSSAPI");
		}
		return new DirectoryUserAdmin(null, properties, true);
	}

	protected DirectoryUserAdmin scopeLdif(User user) {
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
		Dictionary<String, Object> properties = cloneConfigProperties();
		properties.put(DirectoryConf.readOnly.name(), "true");
		DirectoryUserAdmin scopedUserAdmin = new DirectoryUserAdmin(null, properties, true);
//		scopedUserAdmin.groups = Collections.unmodifiableNavigableMap(groups);
//		scopedUserAdmin.users = Collections.unmodifiableNavigableMap(users);
		// FIXME do it better
		((LdifDao) getDirectoryDao()).scope((LdifDao) scopedUserAdmin.getDirectoryDao());
		return scopedUserAdmin;
	}

	@Override
	public String getRolePath(Role role) {
		return nameToRelativePath(LdapNameUtils.toLdapName(role.getName()));
	}

	@Override
	public String getRoleSimpleName(Role role) {
		LdapName dn = LdapNameUtils.toLdapName(role.getName());
		String name = LdapNameUtils.getLastRdnValue(dn);
		return name;
	}

	@Override
	public Role getRoleByPath(String path) {
		return (Role) doGetRole(pathToName(path));
	}

	protected List<Role> getAllRoles(DirectoryUser user) {
		List<Role> allRoles = new ArrayList<Role>();
		if (user != null) {
			collectRoles(user, allRoles);
			allRoles.add(user);
		} else
			collectAnonymousRoles(allRoles);
		return allRoles;
	}

	private void collectRoles(DirectoryUser user, List<Role> allRoles) {
		List<LdapEntry> allEntries = new ArrayList<>();
		LdapEntry entry = (LdapEntry) user;
		collectGroups(entry, allEntries);
		for (LdapEntry e : allEntries) {
			allRoles.add((Role) e);
		}
//		Attributes attrs = user.getAttributes();
//		// TODO centralize attribute name
//		Attribute memberOf = attrs.get(LdapAttrs.memberOf.name());
//		// if user belongs to this directory, we only check memberOf
//		if (memberOf != null && user.getDn().startsWith(getBaseDn())) {
//			try {
//				NamingEnumeration<?> values = memberOf.getAll();
//				while (values.hasMore()) {
//					Object value = values.next();
//					LdapName groupDn = new LdapName(value.toString());
//					DirectoryUser group = doGetRole(groupDn);
//					if (group != null)
//						allRoles.add(group);
//				}
//			} catch (NamingException e) {
//				throw new IllegalStateException("Cannot get memberOf groups for " + user, e);
//			}
//		} else {
//			for (LdapName groupDn : getDirectoryDao().getDirectGroups(user.getDn())) {
//				// TODO check for loops
//				DirectoryUser group = doGetRole(groupDn);
//				if (group != null) {
//					allRoles.add(group);
//					collectRoles(group, allRoles);
//				}
//			}
//		}
	}

	private void collectAnonymousRoles(List<Role> allRoles) {
		// TODO gather anonymous roles
	}

	// USER ADMIN
	@Override
	public Role getRole(String name) {
		return (Role) doGetRole(toLdapName(name));
	}

	@Override
	public Role[] getRoles(String filter) throws InvalidSyntaxException {
		List<? extends Role> res = getRoles(getBaseDn(), filter, true);
		return res.toArray(new Role[res.size()]);
	}

	List<DirectoryUser> getRoles(LdapName searchBase, String filter, boolean deep) throws InvalidSyntaxException {
		LdapEntryWorkingCopy wc = getWorkingCopy();
//		Filter f = filter != null ? FrameworkUtil.createFilter(filter) : null;
		List<LdapEntry> searchRes = getDirectoryDao().doGetEntries(searchBase, filter, deep);
		List<DirectoryUser> res = new ArrayList<>();
		for (LdapEntry entry : searchRes)
			res.add((DirectoryUser) entry);
		if (wc != null) {
			for (Iterator<DirectoryUser> it = res.iterator(); it.hasNext();) {
				DirectoryUser user = (DirectoryUser) it.next();
				LdapName dn = LdapNameUtils.toLdapName(user.getName());
				if (wc.getDeletedData().containsKey(dn))
					it.remove();
			}
			Filter f = filter != null ? FrameworkUtil.createFilter(filter) : null;
			for (LdapEntry ldapEntry : wc.getNewData().values()) {
				DirectoryUser user = (DirectoryUser) ldapEntry;
				if (f == null || f.match(user.getProperties()))
					res.add(user);
			}
			// no need to check modified users,
			// since doGetRoles was already based on the modified attributes
		}

		// if non deep we also search users and groups
//		if (!deep) {
//			try {
//				if (!(searchBase.endsWith(new LdapName(getUserBase()))
//						|| searchBase.endsWith(new LdapName(getGroupBase())))) {
//					LdapName usersBase = (LdapName) ((LdapName) searchBase.clone()).add(getUserBase());
//					res.addAll(getRoles(usersBase, filter, false));
//					LdapName groupsBase = (LdapName) ((LdapName) searchBase.clone()).add(getGroupBase());
//					res.addAll(getRoles(groupsBase, filter, false));
//				}
//			} catch (InvalidNameException e) {
//				throw new IllegalStateException("Cannot search users and groups", e);
//			}
//		}
		return res;
	}

	@Override
	public User getUser(String key, String value) {
		// TODO check value null or empty
		List<DirectoryUser> collectedUsers = new ArrayList<DirectoryUser>();
		if (key != null) {
			doGetUser(key, value, collectedUsers);
		} else {
			throw new IllegalArgumentException("Key cannot be null");
		}

		if (collectedUsers.size() == 1) {
			return collectedUsers.get(0);
		} else if (collectedUsers.size() > 1) {
			// log.warn(collectedUsers.size() + " users for " + (key != null ? key + "=" :
			// "") + value);
		}
		return null;
	}

	protected void doGetUser(String key, String value, List<DirectoryUser> collectedUsers) {
		String f = "(" + key + "=" + value + ")";
		List<LdapEntry> users = getDirectoryDao().doGetEntries(getBaseDn(), f, true);
		for (LdapEntry entry : users)
			collectedUsers.add((DirectoryUser) entry);
	}

	@Override
	public Authorization getAuthorization(User user) {
		if (user == null || user instanceof DirectoryUser) {
			return new LdifAuthorization(user, getAllRoles((DirectoryUser) user));
		} else {
			// bind
			DirectoryUserAdmin scopedUserAdmin = (DirectoryUserAdmin) scope(user);
			try {
				DirectoryUser directoryUser = (DirectoryUser) scopedUserAdmin.getRole(user.getName());
				if (directoryUser == null)
					throw new IllegalStateException("No scoped user found for " + user);
				LdifAuthorization authorization = new LdifAuthorization(directoryUser,
						scopedUserAdmin.getAllRoles(directoryUser));
				return authorization;
			} finally {
				scopedUserAdmin.destroy();
			}
		}
	}

	@Override
	public Role createRole(String name, int type) {
		checkEdit();
		LdapEntryWorkingCopy wc = getWorkingCopy();
		LdapName dn = toLdapName(name);
		if ((getDirectoryDao().entryExists(dn) && !wc.getDeletedData().containsKey(dn))
				|| wc.getNewData().containsKey(dn))
			throw new IllegalArgumentException("Already a role " + name);
		BasicAttributes attrs = new BasicAttributes(true);
		// attrs.put(LdifName.dn.name(), dn.toString());
		Rdn nameRdn = dn.getRdn(dn.size() - 1);
		// TODO deal with multiple attr RDN
		attrs.put(nameRdn.getType(), nameRdn.getValue());
		if (wc.getDeletedData().containsKey(dn)) {
			wc.getDeletedData().remove(dn);
			wc.getModifiedData().put(dn, attrs);
			return getRole(name);
		} else {
			wc.getModifiedData().put(dn, attrs);
			LdapEntry newRole = newRole(dn, type, attrs);
			wc.getNewData().put(dn, newRole);
			return (Role) newRole;
		}
	}

	protected LdapEntry newRole(LdapName dn, int type, Attributes attrs) {
		LdapEntry newRole;
		BasicAttribute objClass = new BasicAttribute(objectClass.name());
		if (type == Role.USER) {
			String userObjClass = getUserObjectClass();
			objClass.add(userObjClass);
			if (inetOrgPerson.name().equals(userObjClass)) {
				objClass.add(organizationalPerson.name());
				objClass.add(person.name());
			} else if (organizationalPerson.name().equals(userObjClass)) {
				objClass.add(person.name());
			}
			objClass.add(top.name());
			objClass.add(extensibleObject.name());
			attrs.put(objClass);
			newRole = newUser(dn, attrs);
		} else if (type == Role.GROUP) {
			String groupObjClass = getGroupObjectClass();
			objClass.add(groupObjClass);
			// objClass.add(LdifName.extensibleObject.name());
			objClass.add(top.name());
			attrs.put(objClass);
			newRole = newGroup(dn, attrs);
		} else
			throw new IllegalArgumentException("Unsupported type " + type);
		return newRole;
	}

	@Override
	public boolean removeRole(String name) {
		return removeEntry(LdapNameUtils.toLdapName(name));
//		checkEdit();
//		LdapEntryWorkingCopy wc = getWorkingCopy();
//		LdapName dn = toLdapName(name);
//		boolean actuallyDeleted;
//		if (getDirectoryDao().daoHasEntry(dn) || wc.getNewData().containsKey(dn)) {
//			DirectoryUser user = (DirectoryUser) getRole(name);
//			wc.getDeletedData().put(dn, user);
//			actuallyDeleted = true;
//		} else {// just removing from groups (e.g. system roles)
//			actuallyDeleted = false;
//		}
//		for (LdapName groupDn : getDirectoryDao().getDirectGroups(dn)) {
//			LdapEntry group = doGetRole(groupDn);
//			group.getAttributes().get(getMemberAttributeId()).remove(dn.toString());
//		}
//		return actuallyDeleted;
	}

	/*
	 * HIERARCHY
	 */
	@Override
	public HierarchyUnit getHierarchyUnit(Role role) {
		LdapName dn = LdapNameUtils.toLdapName(role.getName());
		LdapName huDn = LdapNameUtils.getParent(dn);
		HierarchyUnit hierarchyUnit = getDirectoryDao().doGetHierarchyUnit(huDn);
		if (hierarchyUnit == null)
			throw new IllegalStateException("No hierarchy unit found for " + role);
		return hierarchyUnit;
	}

	@Override
	public Iterable<? extends Role> getHierarchyUnitRoles(HierarchyUnit hierarchyUnit, String filter, boolean deep) {
		LdapName dn = LdapNameUtils.toLdapName(hierarchyUnit.getContext());
		try {
			return getRoles(dn, filter, deep);
		} catch (InvalidSyntaxException e) {
			throw new IllegalArgumentException("Cannot filter " + filter + " " + dn, e);
		}
	}

	/*
	 * ROLES CREATION
	 */
	protected LdapEntry newUser(LdapName name, Attributes attrs) {
		// TODO support devices, applications, etc.
		return new LdifUser(this, name, attrs);
	}

	protected LdapEntry newGroup(LdapName name, Attributes attrs) {
		return new LdifGroup(this, name, attrs);

	}

	// GETTERS
	protected UserAdmin getExternalRoles() {
		return externalRoles;
	}

	public void setExternalRoles(UserAdmin externalRoles) {
		this.externalRoles = externalRoles;
	}

//	public void setTransactionManager(TransactionManager transactionManager) {
//		this.transactionManager = transactionManager;
//	}

	/*
	 * STATIC UTILITIES
	 */
	static LdapName toLdapName(String name) {
		try {
			return new LdapName(name);
		} catch (InvalidNameException e) {
			throw new IllegalArgumentException(name + " is not an LDAP name", e);
		}
	}
}