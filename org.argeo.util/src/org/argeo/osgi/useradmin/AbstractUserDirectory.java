package org.argeo.osgi.useradmin;

import static org.argeo.util.naming.LdapAttrs.objectClass;
import static org.argeo.util.naming.LdapObjs.extensibleObject;
import static org.argeo.util.naming.LdapObjs.inetOrgPerson;
import static org.argeo.util.naming.LdapObjs.organizationalPerson;
import static org.argeo.util.naming.LdapObjs.person;
import static org.argeo.util.naming.LdapObjs.top;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import javax.naming.InvalidNameException;
import javax.naming.NameNotFoundException;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;

import org.argeo.osgi.transaction.WorkControl;
import org.argeo.util.naming.LdapAttrs;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.useradmin.Authorization;
import org.osgi.service.useradmin.Role;
import org.osgi.service.useradmin.User;
import org.osgi.service.useradmin.UserAdmin;

/** Base class for a {@link UserDirectory}. */
abstract class AbstractUserDirectory implements UserAdmin, UserDirectory {
	static final String SHARED_STATE_USERNAME = "javax.security.auth.login.name";
	static final String SHARED_STATE_PASSWORD = "javax.security.auth.login.password";

	private final Hashtable<String, Object> properties;
	private final LdapName baseDn, userBaseDn, groupBaseDn;
	private final String userObjectClass, userBase, groupObjectClass, groupBase;

	private final boolean readOnly;
	private final boolean disabled;
	private final String uri;

	private UserAdmin externalRoles;
	// private List<String> indexedUserProperties = Arrays
	// .asList(new String[] { LdapAttrs.uid.name(), LdapAttrs.mail.name(),
	// LdapAttrs.cn.name() });

	private final boolean scoped;

	private String memberAttributeId = "member";
	private List<String> credentialAttributeIds = Arrays
			.asList(new String[] { LdapAttrs.userPassword.name(), LdapAttrs.authPassword.name() });

	// Transaction
//	private TransactionManager transactionManager;
	private WorkControl transactionControl;
	private WcXaResource xaResource = new WcXaResource(this);

	private String forcedPassword;

	AbstractUserDirectory(URI uriArg, Dictionary<String, ?> props, boolean scoped) {
		this.scoped = scoped;
		properties = new Hashtable<String, Object>();
		for (Enumeration<String> keys = props.keys(); keys.hasMoreElements();) {
			String key = keys.nextElement();
			properties.put(key, props.get(key));
		}

		if (uriArg != null) {
			uri = uriArg.toString();
			// uri from properties is ignored
		} else {
			String uriStr = UserAdminConf.uri.getValue(properties);
			if (uriStr == null)
				uri = null;
			else
				uri = uriStr;
		}

		forcedPassword = UserAdminConf.forcedPassword.getValue(properties);

		userObjectClass = UserAdminConf.userObjectClass.getValue(properties);
		userBase = UserAdminConf.userBase.getValue(properties);
		groupObjectClass = UserAdminConf.groupObjectClass.getValue(properties);
		groupBase = UserAdminConf.groupBase.getValue(properties);
		try {
			baseDn = new LdapName(UserAdminConf.baseDn.getValue(properties));
			userBaseDn = new LdapName(userBase + "," + baseDn);
			groupBaseDn = new LdapName(groupBase + "," + baseDn);
		} catch (InvalidNameException e) {
			throw new IllegalArgumentException("Badly formated base DN " + UserAdminConf.baseDn.getValue(properties),
					e);
		}
		String readOnlyStr = UserAdminConf.readOnly.getValue(properties);
		if (readOnlyStr == null) {
			readOnly = readOnlyDefault(uri);
			properties.put(UserAdminConf.readOnly.name(), Boolean.toString(readOnly));
		} else
			readOnly = Boolean.parseBoolean(readOnlyStr);
		String disabledStr = UserAdminConf.disabled.getValue(properties);
		if (disabledStr != null)
			disabled = Boolean.parseBoolean(disabledStr);
		else
			disabled = false;
	}

	/** Returns the groups this user is a direct member of. */
	protected abstract List<LdapName> getDirectGroups(LdapName dn);

	protected abstract Boolean daoHasRole(LdapName dn);

	protected abstract DirectoryUser daoGetRole(LdapName key) throws NameNotFoundException;

	protected abstract List<DirectoryUser> doGetRoles(LdapName searchBase, Filter f, boolean deep);

	protected abstract AbstractUserDirectory scope(User user);

	public void init() {

	}

	public void destroy() {

	}

	@Override
	public String getBasePath() {
		return getBaseDn().toString();
	}

	@Override
	public Optional<String> getRealm() {
		Object realm = getProperties().get(UserAdminConf.realm.name());
		if (realm == null)
			return Optional.empty();
		return Optional.of(realm.toString());
	}

	protected boolean isEditing() {
		return xaResource.wc() != null;
	}

	protected UserDirectoryWorkingCopy getWorkingCopy() {
		UserDirectoryWorkingCopy wc = xaResource.wc();
		if (wc == null)
			return null;
		return wc;
	}

	protected void checkEdit() {
		if (xaResource.wc() == null) {
			try {
				transactionControl.getWorkContext().registerXAResource(xaResource, null);
			} catch (Exception e) {
				throw new IllegalStateException("Cannot enlist " + xaResource, e);
			}
		} else {
		}
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
		Attributes attrs = user.getAttributes();
		// TODO centralize attribute name
		Attribute memberOf = attrs.get(LdapAttrs.memberOf.name());
		// if user belongs to this directory, we only check meberOf
		if (memberOf != null && user.getDn().startsWith(getBaseDn())) {
			try {
				NamingEnumeration<?> values = memberOf.getAll();
				while (values.hasMore()) {
					Object value = values.next();
					LdapName groupDn = new LdapName(value.toString());
					DirectoryUser group = doGetRole(groupDn);
					if (group != null)
						allRoles.add(group);
				}
			} catch (NamingException e) {
				throw new IllegalStateException("Cannot get memberOf groups for " + user, e);
			}
		} else {
			for (LdapName groupDn : getDirectGroups(user.getDn())) {
				// TODO check for loops
				DirectoryUser group = doGetRole(groupDn);
				if (group != null) {
					allRoles.add(group);
					collectRoles(group, allRoles);
				}
			}
		}
	}

	private void collectAnonymousRoles(List<Role> allRoles) {
		// TODO gather anonymous roles
	}

	// USER ADMIN
	@Override
	public Role getRole(String name) {
		return doGetRole(toLdapName(name));
	}

	protected DirectoryUser doGetRole(LdapName dn) {
		UserDirectoryWorkingCopy wc = getWorkingCopy();
		DirectoryUser user;
		try {
			user = daoGetRole(dn);
		} catch (NameNotFoundException e) {
			user = null;
		}
		if (wc != null) {
			if (user == null && wc.getNewUsers().containsKey(dn))
				user = wc.getNewUsers().get(dn);
			else if (wc.getDeletedUsers().containsKey(dn))
				user = null;
		}
		return user;
	}

	@Override
	public Role[] getRoles(String filter) throws InvalidSyntaxException {
//		UserDirectoryWorkingCopy wc = getWorkingCopy();
//		Filter f = filter != null ? FrameworkUtil.createFilter(filter) : null;
//		List<DirectoryUser> res = doGetRoles(getBaseDn(), f, true);
//		if (wc != null) {
//			for (Iterator<DirectoryUser> it = res.iterator(); it.hasNext();) {
//				DirectoryUser user = it.next();
//				LdapName dn = user.getDn();
//				if (wc.getDeletedUsers().containsKey(dn))
//					it.remove();
//			}
//			for (DirectoryUser user : wc.getNewUsers().values()) {
//				if (f == null || f.match(user.getProperties()))
//					res.add(user);
//			}
//			// no need to check modified users,
//			// since doGetRoles was already based on the modified attributes
//		}
		List<? extends Role> res = getRoles(getBaseDn(), filter, true);
		return res.toArray(new Role[res.size()]);
	}

	List<DirectoryUser> getRoles(LdapName searchBase, String filter, boolean deep) throws InvalidSyntaxException {
		UserDirectoryWorkingCopy wc = getWorkingCopy();
		Filter f = filter != null ? FrameworkUtil.createFilter(filter) : null;
		List<DirectoryUser> res = doGetRoles(searchBase, f, deep);
		if (wc != null) {
			for (Iterator<DirectoryUser> it = res.iterator(); it.hasNext();) {
				DirectoryUser user = it.next();
				LdapName dn = user.getDn();
				if (wc.getDeletedUsers().containsKey(dn))
					it.remove();
			}
			for (DirectoryUser user : wc.getNewUsers().values()) {
				if (f == null || f.match(user.getProperties()))
					res.add(user);
			}
			// no need to check modified users,
			// since doGetRoles was already based on the modified attributes
		}

		// if non deep we also search users and groups
		if (!deep) {
			try {
				if (!(searchBase.endsWith(new LdapName(getUserBase()))
						|| searchBase.endsWith(new LdapName(getGroupBase())))) {
					LdapName usersBase = (LdapName) ((LdapName) searchBase.clone()).add(getUserBase());
					res.addAll(getRoles(usersBase, filter, false));
					LdapName groupsBase = (LdapName) ((LdapName) searchBase.clone()).add(getGroupBase());
					res.addAll(getRoles(groupsBase, filter, false));
				}
			} catch (InvalidNameException e) {
				throw new IllegalStateException("Cannot search users and groups", e);
			}
		}
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
		try {
			Filter f = FrameworkUtil.createFilter("(" + key + "=" + value + ")");
			List<DirectoryUser> users = doGetRoles(getBaseDn(), f, true);
			collectedUsers.addAll(users);
		} catch (InvalidSyntaxException e) {
			throw new IllegalArgumentException("Cannot get user with " + key + "=" + value, e);
		}
	}

	@Override
	public Authorization getAuthorization(User user) {
		if (user == null || user instanceof DirectoryUser) {
			return new LdifAuthorization(user, getAllRoles((DirectoryUser) user));
		} else {
			// bind
			AbstractUserDirectory scopedUserAdmin = scope(user);
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
		UserDirectoryWorkingCopy wc = getWorkingCopy();
		LdapName dn = toLdapName(name);
		if ((daoHasRole(dn) && !wc.getDeletedUsers().containsKey(dn)) || wc.getNewUsers().containsKey(dn))
			throw new IllegalArgumentException("Already a role " + name);
		BasicAttributes attrs = new BasicAttributes(true);
		// attrs.put(LdifName.dn.name(), dn.toString());
		Rdn nameRdn = dn.getRdn(dn.size() - 1);
		// TODO deal with multiple attr RDN
		attrs.put(nameRdn.getType(), nameRdn.getValue());
		if (wc.getDeletedUsers().containsKey(dn)) {
			wc.getDeletedUsers().remove(dn);
			wc.getModifiedUsers().put(dn, attrs);
			return getRole(name);
		} else {
			wc.getModifiedUsers().put(dn, attrs);
			DirectoryUser newRole = newRole(dn, type, attrs);
			wc.getNewUsers().put(dn, newRole);
			return newRole;
		}
	}

	protected DirectoryUser newRole(LdapName dn, int type, Attributes attrs) {
		LdifUser newRole;
		BasicAttribute objClass = new BasicAttribute(objectClass.name());
		if (type == Role.USER) {
			String userObjClass = newUserObjectClass(dn);
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
			newRole = new LdifUser(this, dn, attrs);
		} else if (type == Role.GROUP) {
			String groupObjClass = getGroupObjectClass();
			objClass.add(groupObjClass);
			// objClass.add(LdifName.extensibleObject.name());
			objClass.add(top.name());
			attrs.put(objClass);
			newRole = new LdifGroup(this, dn, attrs);
		} else
			throw new IllegalArgumentException("Unsupported type " + type);
		return newRole;
	}

	@Override
	public boolean removeRole(String name) {
		checkEdit();
		UserDirectoryWorkingCopy wc = getWorkingCopy();
		LdapName dn = toLdapName(name);
		boolean actuallyDeleted;
		if (daoHasRole(dn) || wc.getNewUsers().containsKey(dn)) {
			DirectoryUser user = (DirectoryUser) getRole(name);
			wc.getDeletedUsers().put(dn, user);
			actuallyDeleted = true;
		} else {// just removing from groups (e.g. system roles)
			actuallyDeleted = false;
		}
		for (LdapName groupDn : getDirectGroups(dn)) {
			DirectoryUser group = doGetRole(groupDn);
			group.getAttributes().get(getMemberAttributeId()).remove(dn.toString());
		}
		return actuallyDeleted;
	}

	// TRANSACTION
	protected void prepare(UserDirectoryWorkingCopy wc) {

	}

	protected void commit(UserDirectoryWorkingCopy wc) {

	}

	protected void rollback(UserDirectoryWorkingCopy wc) {

	}

	/*
	 * HIERARCHY
	 */
	@Override
	public int getHierarchyChildCount() {
		return 0;
	}

	@Override
	public HierarchyUnit getHierarchyChild(int i) {
		throw new IllegalArgumentException("No child hierarchy unit available");
	}

	@Override
	public HierarchyUnit getParent() {
		return null;
	}

	@Override
	public int getHierarchyUnitType() {
		return 0;
	}

	@Override
	public String getHierarchyUnitName() {
		String name = LdapNameUtils.getLastRdnAsString(baseDn);
		// TODO check ou, o, etc.
		return name;
	}

	@Override
	public HierarchyUnit getHierarchyUnit(String path) {
		return null;
	}

	@Override
	public HierarchyUnit getHierarchyUnit(Role role) {
		return null;
	}

	@Override
	public List<? extends Role> getRoles(String filter, boolean deep) {
		try {
			return getRoles(getBaseDn(), filter, deep);
		} catch (InvalidSyntaxException e) {
			throw new IllegalArgumentException("Cannot filter " + filter + " " + getBaseDn(), e);
		}
	}

	// GETTERS
	protected String getMemberAttributeId() {
		return memberAttributeId;
	}

	protected List<String> getCredentialAttributeIds() {
		return credentialAttributeIds;
	}

	protected String getUri() {
		return uri;
	}

	private static boolean readOnlyDefault(String uriStr) {
		if (uriStr == null)
			return true;
		/// TODO make it more generic
		URI uri;
		try {
			uri = new URI(uriStr.split(" ")[0]);
		} catch (URISyntaxException e) {
			throw new IllegalArgumentException(e);
		}
		if (uri.getScheme() == null)
			return false;// assume relative file to be writable
		if (uri.getScheme().equals(UserAdminConf.SCHEME_FILE)) {
			File file = new File(uri);
			if (file.exists())
				return !file.canWrite();
			else
				return !file.getParentFile().canWrite();
		} else if (uri.getScheme().equals(UserAdminConf.SCHEME_LDAP)) {
			if (uri.getAuthority() != null)// assume writable if authenticated
				return false;
		} else if (uri.getScheme().equals(UserAdminConf.SCHEME_OS)) {
			return true;
		}
		return true;// read only by default
	}

	public boolean isReadOnly() {
		return readOnly;
	}

	public boolean isDisabled() {
		return disabled;
	}

	protected UserAdmin getExternalRoles() {
		return externalRoles;
	}

	protected int roleType(LdapName dn) {
		if (dn.startsWith(groupBaseDn))
			return Role.GROUP;
		else if (dn.startsWith(userBaseDn))
			return Role.USER;
		else
			return Role.GROUP;
	}

	/** dn can be null, in that case a default should be returned. */
	public String getUserObjectClass() {
		return userObjectClass;
	}

	public String getUserBase() {
		return userBase;
	}

	protected String newUserObjectClass(LdapName dn) {
		return getUserObjectClass();
	}

	public String getGroupObjectClass() {
		return groupObjectClass;
	}

	public String getGroupBase() {
		return groupBase;
	}

	public LdapName getBaseDn() {
		return (LdapName) baseDn.clone();
	}

	public Dictionary<String, Object> getProperties() {
		return properties;
	}

	public Dictionary<String, Object> cloneProperties() {
		return new Hashtable<>(properties);
	}

	public void setExternalRoles(UserAdmin externalRoles) {
		this.externalRoles = externalRoles;
	}

//	public void setTransactionManager(TransactionManager transactionManager) {
//		this.transactionManager = transactionManager;
//	}

	public String getForcedPassword() {
		return forcedPassword;
	}

	public void setTransactionControl(WorkControl transactionControl) {
		this.transactionControl = transactionControl;
	}

	public WcXaResource getXaResource() {
		return xaResource;
	}

	public boolean isScoped() {
		return scoped;
	}

	@Override
	public int hashCode() {
		return baseDn.hashCode();
	}

	@Override
	public String toString() {
		return "User Directory " + baseDn.toString();
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
