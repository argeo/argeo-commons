package org.argeo.cms.osgi.useradmin;

import static org.argeo.api.acr.ldap.LdapAttr.objectClass;
import static org.argeo.api.acr.ldap.LdapObj.extensibleObject;
import static org.argeo.api.acr.ldap.LdapObj.inetOrgPerson;
import static org.argeo.api.acr.ldap.LdapObj.organizationalPerson;
import static org.argeo.api.acr.ldap.LdapObj.person;
import static org.argeo.api.acr.ldap.LdapObj.top;

import java.net.URI;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import javax.naming.Context;
import javax.naming.InvalidNameException;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;
import javax.security.auth.Subject;
import javax.security.auth.kerberos.KerberosTicket;

import org.argeo.api.acr.ldap.LdapNameUtils;
import org.argeo.api.cms.directory.CmsRole;
import org.argeo.api.cms.directory.DirectoryDigestUtils;
import org.argeo.api.cms.directory.HierarchyUnit;
import org.argeo.api.cms.directory.UserDirectory;
import org.argeo.cms.directory.ldap.AbstractLdapDirectory;
import org.argeo.cms.directory.ldap.LdapDao;
import org.argeo.cms.directory.ldap.LdapEntry;
import org.argeo.cms.directory.ldap.LdapEntryWorkingCopy;
import org.argeo.cms.directory.ldap.LdifDao;
import org.argeo.cms.runtime.DirectoryConf;
import org.argeo.cms.util.CurrentSubject;
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

	// Transaction
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

	protected Optional<DirectoryUserAdmin> scope(User user) {
		if (getDirectoryDao() instanceof LdapDao) {
			return scopeLdap(user);
		} else if (getDirectoryDao() instanceof LdifDao) {
			return scopeLdif(user);
		} else {
			throw new IllegalStateException("Unsupported DAO " + getDirectoryDao().getClass());
		}
	}

	protected Optional<DirectoryUserAdmin> scopeLdap(User user) {
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
		DirectoryUserAdmin scopedDirectory = new DirectoryUserAdmin(null, properties, true);
		scopedDirectory.init();
		// check connection
		if (!scopedDirectory.getDirectoryDao().checkConnection())
			return Optional.empty();
		return Optional.of(scopedDirectory);
	}

	protected Optional<DirectoryUserAdmin> scopeLdif(User user) {
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
		// FIXME do it better
		((LdifDao) getDirectoryDao()).scope((LdifDao) scopedUserAdmin.getDirectoryDao());
		// no need to check authentication
		scopedUserAdmin.init();
		return Optional.of(scopedUserAdmin);
	}

	@Override
	public String getRolePath(CmsRole role) {
		return nameToRelativePath(LdapNameUtils.toLdapName(role.getName()));
	}

	@Override
	public String getRoleSimpleName(CmsRole role) {
		LdapName dn = LdapNameUtils.toLdapName(role.getName());
		String name = LdapNameUtils.getLastRdnValue(dn);
		return name;
	}

	@Override
	public CmsRole getRoleByPath(String path) {
		LdapEntry entry = doGetRole(pathToName(path));
		if (!(entry instanceof CmsRole)) {
			return null;
//			throw new IllegalStateException("Path must be a UserAdmin Role.");
		} else {
			return (CmsRole) entry;
		}
	}

	protected List<CmsOsgiRole> getAllRoles(CmsOsgiUser user) {
		List<CmsOsgiRole> allRoles = new ArrayList<CmsOsgiRole>();
		if (user != null) {
			collectRoles((LdapEntry) user, allRoles);
			allRoles.add(user);
		} else
			collectAnonymousRoles(allRoles);
		return allRoles;
	}

	private void collectRoles(LdapEntry user, List<CmsOsgiRole> allRoles) {
		List<LdapEntry> allEntries = new ArrayList<>();
		LdapEntry entry = user;
		collectGroups(entry, allEntries);
		for (LdapEntry e : allEntries) {
			if (e instanceof CmsOsgiRole)
				allRoles.add((CmsOsgiRole) e);
		}
	}

	private void collectAnonymousRoles(List<CmsOsgiRole> allRoles) {
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

	List<CmsOsgiUser> getRoles(LdapName searchBase, String filter, boolean deep) throws InvalidSyntaxException {
		LdapEntryWorkingCopy wc = getWorkingCopy();
//		Filter f = filter != null ? FrameworkUtil.createFilter(filter) : null;
		List<LdapEntry> searchRes = getDirectoryDao().doGetEntries(searchBase, filter, deep);
		List<CmsOsgiUser> res = new ArrayList<>();
		for (LdapEntry entry : searchRes)
			res.add((CmsOsgiUser) entry);
		if (wc != null) {
			for (Iterator<CmsOsgiUser> it = res.iterator(); it.hasNext();) {
				CmsOsgiUser user = it.next();
				LdapName dn = LdapNameUtils.toLdapName(user.getName());
				if (wc.getDeletedData().containsKey(dn))
					it.remove();
			}
			Filter f = filter != null ? FrameworkUtil.createFilter(filter) : null;
			for (LdapEntry ldapEntry : wc.getNewData().values()) {
				CmsOsgiUser user = (CmsOsgiUser) ldapEntry;
				if (f == null || f.match(user.getProperties()))
					res.add(user);
			}
			// no need to check modified users,
			// since doGetRoles was already based on the modified attributes
		}
		return res;
	}

	@Override
	public User getUser(String key, String value) {
		// TODO check value null or empty
		List<CmsOsgiUser> collectedUsers = new ArrayList<>();
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

	protected void doGetUser(String key, String value, List<CmsOsgiUser> collectedUsers) {
		String f = "(" + key + "=" + value + ")";
		List<LdapEntry> users = getDirectoryDao().doGetEntries(getBaseDn(), f, true);
		for (LdapEntry entry : users)
			collectedUsers.add((CmsOsgiUser) entry);
	}

	@Override
	public Authorization getAuthorization(User user) {
		if (user == null) {// anonymous
			return new LdifAuthorization(user, getAllRoles(null));
		}
		LdapName userName = toLdapName(user.getName());
		if (isExternal(userName) && user instanceof LdapEntry ldapEntry) {
			List<CmsOsgiRole> allRoles = new ArrayList<>();
			collectRoles(ldapEntry, allRoles);
			return new LdifAuthorization(user, allRoles);
		} else {

			Subject currentSubject = CurrentSubject.current();
			if (currentSubject != null //
					&& getRealm().isPresent() //
					&& !currentSubject.getPrivateCredentials(Authorization.class).isEmpty() //
					&& !currentSubject.getPrivateCredentials(KerberosTicket.class).isEmpty()) //
			{
				// TODO not only Kerberos but also bind scope with kept password ?
				Authorization auth = currentSubject.getPrivateCredentials(Authorization.class).iterator().next();
				// bind with authenticating user
				DirectoryUserAdmin scopedUserAdmin = CurrentSubject.callAs(currentSubject, () -> {
					return scope(new AuthenticatingUser(auth.getName(), new Hashtable<>())).orElseThrow();
				});
				return getAuthorizationFromScoped(scopedUserAdmin, user);
			}

			if (user instanceof CmsOsgiUser u) {
				return new LdifAuthorization(user, getAllRoles(u));
			} else {
				// bind with authenticating user
				DirectoryUserAdmin scopedUserAdmin = scope(user).orElseThrow();
				return getAuthorizationFromScoped(scopedUserAdmin, user);
			}
		}
	}

	private Authorization getAuthorizationFromScoped(DirectoryUserAdmin scopedUserAdmin, User user) {
		try {
			CmsOsgiUser directoryUser = (CmsOsgiUser) scopedUserAdmin.getRole(user.getName());
			if (directoryUser == null)
				throw new IllegalStateException("No scoped user found for " + user);
			LdifAuthorization authorization = new LdifAuthorization(directoryUser,
					scopedUserAdmin.getAllRoles(directoryUser));
			return authorization;
		} finally {
			scopedUserAdmin.destroy();
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
			LdapEntry newRole = doCreateRole(dn, type, attrs);
			wc.getNewData().put(dn, newRole);
			return (Role) newRole;
		}
	}

	private LdapEntry doCreateRole(LdapName dn, int type, Attributes attrs) {
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
			newRole = newUser(dn);
		} else if (type == Role.GROUP) {
			String groupObjClass = getGroupObjectClass();
			objClass.add(groupObjClass);
			// objClass.add(LdifName.extensibleObject.name());
			objClass.add(top.name());
			attrs.put(objClass);
			newRole = newGroup(dn);
		} else
			throw new IllegalArgumentException("Unsupported type " + type);
		return newRole;
	}

	@Override
	public boolean removeRole(String name) {
		return removeEntry(LdapNameUtils.toLdapName(name));
	}

	/*
	 * HIERARCHY
	 */
	@Override
	public HierarchyUnit getHierarchyUnit(CmsRole role) {
		LdapName dn = LdapNameUtils.toLdapName(role.getName());
		LdapName huDn = LdapNameUtils.getParent(dn);
		HierarchyUnit hierarchyUnit = getDirectoryDao().doGetHierarchyUnit(huDn);
		if (hierarchyUnit == null)
			throw new IllegalStateException("No hierarchy unit found for " + role);
		return hierarchyUnit;
	}

	@Override
	public Iterable<? extends CmsRole> getHierarchyUnitRoles(HierarchyUnit hierarchyUnit, String filter, boolean deep) {
		LdapName dn = LdapNameUtils.toLdapName(hierarchyUnit.getBase());
		try {
			return getRoles(dn, filter, deep);
		} catch (InvalidSyntaxException e) {
			throw new IllegalArgumentException("Cannot filter " + filter + " " + dn, e);
		}
	}

	/*
	 * ROLES CREATION
	 */
	protected LdapEntry newUser(LdapName name) {
		// TODO support devices, applications, etc.
		return new LdifUser(this, name);
	}

	protected LdapEntry newGroup(LdapName name) {
		return new LdifGroup(this, name);

	}

	// GETTERS
	protected UserAdmin getExternalRoles() {
		return externalRoles;
	}

	public void setExternalRoles(UserAdmin externalRoles) {
		this.externalRoles = externalRoles;
	}

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
