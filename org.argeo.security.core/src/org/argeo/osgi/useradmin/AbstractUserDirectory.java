package org.argeo.osgi.useradmin;

import static org.argeo.osgi.useradmin.LdifName.gidNumber;
import static org.argeo.osgi.useradmin.LdifName.homeDirectory;
import static org.argeo.osgi.useradmin.LdifName.inetOrgPerson;
import static org.argeo.osgi.useradmin.LdifName.objectClass;
import static org.argeo.osgi.useradmin.LdifName.organizationalPerson;
import static org.argeo.osgi.useradmin.LdifName.person;
import static org.argeo.osgi.useradmin.LdifName.posixAccount;
import static org.argeo.osgi.useradmin.LdifName.top;
import static org.argeo.osgi.useradmin.LdifName.uid;
import static org.argeo.osgi.useradmin.LdifName.uidNumber;

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

import javax.naming.InvalidNameException;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.argeo.ArgeoException;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.useradmin.Authorization;
import org.osgi.service.useradmin.Role;
import org.osgi.service.useradmin.User;
import org.osgi.service.useradmin.UserAdmin;

/** Base class for a {@link UserDirectory}. */
abstract class AbstractUserDirectory implements UserAdmin, UserDirectory {
	private final static Log log = LogFactory
			.getLog(AbstractUserDirectory.class);

	private final Hashtable<String, Object> properties;
	private final String baseDn;
	private final String userObjectClass;
	private final String groupObjectClass;

	private final boolean readOnly;
	private final URI uri;

	private UserAdmin externalRoles;
	private List<String> indexedUserProperties = Arrays.asList(new String[] {
			LdifName.uid.name(), LdifName.mail.name(), LdifName.cn.name() });

	private String memberAttributeId = "member";
	private List<String> credentialAttributeIds = Arrays
			.asList(new String[] { LdifName.userPassword.name() });

	private TransactionManager transactionManager;
	// private TransactionSynchronizationRegistry transactionRegistry;
	// private Xid editingTransactionXid = null;
	private WcXaResource xaResource = new WcXaResource(this);

	// POSIX
	private String homeDirectoryBase = "/home";

	AbstractUserDirectory(Dictionary<String, ?> props) {
		properties = new Hashtable<String, Object>();
		for (Enumeration<String> keys = props.keys(); keys.hasMoreElements();) {
			String key = keys.nextElement();
			properties.put(key, props.get(key));
		}

		String uriStr = UserAdminConf.uri.getValue(properties);
		if (uriStr == null)
			uri = null;
		else
			try {
				uri = new URI(uriStr);
			} catch (URISyntaxException e) {
				throw new UserDirectoryException("Badly formatted URI "
						+ uriStr, e);
			}

		baseDn = UserAdminConf.baseDn.getValue(properties).toString();
		String readOnlyStr = UserAdminConf.readOnly.getValue(properties);
		if (readOnlyStr == null) {
			readOnly = readOnlyDefault(uri);
			properties.put(UserAdminConf.readOnly.property(),
					Boolean.toString(readOnly));
		} else
			readOnly = new Boolean(readOnlyStr);

		userObjectClass = UserAdminConf.userObjectClass.getValue(properties);
		groupObjectClass = UserAdminConf.groupObjectClass.getValue(properties);
	}

	/** Returns the groups this user is a direct member of. */
	protected abstract List<LdapName> getDirectGroups(LdapName dn);

	protected abstract Boolean daoHasRole(LdapName dn);

	protected abstract DirectoryUser daoGetRole(LdapName key);

	protected abstract List<DirectoryUser> doGetRoles(Filter f);

	public void init() {

	}

	public void destroy() {

	}

	boolean isEditing() {
		// if (editingTransactionXid == null)
		// return false;
		// return workingCopy.get() != null;
		return xaResource.wc() != null;
	}

	protected UserDirectoryWorkingCopy getWorkingCopy() {
		// UserDirectoryWorkingCopy wc = workingCopy.get();
		UserDirectoryWorkingCopy wc = xaResource.wc();
		if (wc == null)
			return null;
		// if (wc.getXid() == null) {
		// workingCopy.set(null);
		// return null;
		// }
		return wc;
	}

	void checkEdit() {
		Transaction transaction;
		try {
			transaction = transactionManager.getTransaction();
		} catch (SystemException e) {
			throw new UserDirectoryException("Cannot get transaction", e);
		}
		if (transaction == null)
			throw new UserDirectoryException(
					"A transaction needs to be active in order to edit");
		if (xaResource.wc() == null) {
			// UserDirectoryWorkingCopy wc = new UserDirectoryWorkingCopy(this);
			try {
				transaction.enlistResource(xaResource);
				// editingTransactionXid = wc.getXid();
				// workingCopy.set(wc);
			} catch (Exception e) {
				throw new UserDirectoryException("Cannot enlist " + xaResource,
						e);
			}
		} else {
			// UserDirectoryWorkingCopy wc = xaResource.wc();
			// if (wc == null)
			// throw new UserDirectoryException("Transaction "
			// + editingTransactionXid + " already editing");
			// else if
			// (!editingTransactionXid.equals(workingCopy.get().getXid()))
			// throw new UserDirectoryException("Working copy Xid "
			// + workingCopy.get().getXid() + " inconsistent with"
			// + editingTransactionXid);
		}
	}

	List<Role> getAllRoles(DirectoryUser user) {
		List<Role> allRoles = new ArrayList<Role>();
		if (user != null) {
			collectRoles(user, allRoles);
			allRoles.add(user);
		} else
			collectAnonymousRoles(allRoles);
		return allRoles;
	}

	private void collectRoles(DirectoryUser user, List<Role> allRoles) {
		for (LdapName groupDn : getDirectGroups(user.getDn())) {
			// TODO check for loops
			DirectoryUser group = doGetRole(groupDn);
			allRoles.add(group);
			collectRoles(group, allRoles);
		}
	}

	private void collectAnonymousRoles(List<Role> allRoles) {
		// TODO gather anonymous roles
	}

	// USER ADMIN
	@Override
	public Role getRole(String name) {
		return doGetRole(toDn(name));
	}

	protected DirectoryUser doGetRole(LdapName dn) {
		UserDirectoryWorkingCopy wc = getWorkingCopy();
		DirectoryUser user = daoGetRole(dn);
		if (wc != null) {
			if (user == null && wc.getNewUsers().containsKey(dn))
				user = wc.getNewUsers().get(dn);
			else if (wc.getDeletedUsers().containsKey(dn))
				user = null;
		}
		return user;
	}

	@SuppressWarnings("unchecked")
	@Override
	public Role[] getRoles(String filter) throws InvalidSyntaxException {
		UserDirectoryWorkingCopy wc = getWorkingCopy();
		Filter f = filter != null ? FrameworkUtil.createFilter(filter) : null;
		List<DirectoryUser> res = doGetRoles(f);
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
		return res.toArray(new Role[res.size()]);
	}

	@Override
	public User getUser(String key, String value) {
		// TODO check value null or empty
		List<DirectoryUser> collectedUsers = new ArrayList<DirectoryUser>(
				getIndexedUserProperties().size());
		if (key != null) {
			doGetUser(key, value, collectedUsers);
		} else {
			// try dn
			DirectoryUser user = null;
			try {
				user = (DirectoryUser) getRole(value);
				if (user != null)
					collectedUsers.add(user);
			} catch (Exception e) {
				// silent
			}
			// try all indexes
			for (String attr : getIndexedUserProperties())
				doGetUser(attr, value, collectedUsers);
		}
		if (collectedUsers.size() == 1)
			return collectedUsers.get(0);
		else if (collectedUsers.size() > 1)
			log.warn(collectedUsers.size() + " users for "
					+ (key != null ? key + "=" : "") + value);
		return null;
	}

	protected void doGetUser(String key, String value,
			List<DirectoryUser> collectedUsers) {
		try {
			Filter f = FrameworkUtil
					.createFilter("(" + key + "=" + value + ")");
			List<DirectoryUser> users = doGetRoles(f);
			collectedUsers.addAll(users);
		} catch (InvalidSyntaxException e) {
			throw new UserDirectoryException("Cannot get user with " + key
					+ "=" + value, e);
		}
	}

	@Override
	public Authorization getAuthorization(User user) {
		return new LdifAuthorization((DirectoryUser) user,
				getAllRoles((DirectoryUser) user));
	}

	@Override
	public Role createRole(String name, int type) {
		checkEdit();
		UserDirectoryWorkingCopy wc = getWorkingCopy();
		LdapName dn = toDn(name);
		if ((daoHasRole(dn) && !wc.getDeletedUsers().containsKey(dn))
				|| wc.getNewUsers().containsKey(dn))
			throw new UserDirectoryException("Already a role " + name);
		BasicAttributes attrs = new BasicAttributes(true);
		attrs.put("dn", dn.toString());
		Rdn nameRdn = dn.getRdn(dn.size() - 1);
		// TODO deal with multiple attr RDN
		attrs.put(nameRdn.getType(), nameRdn.getValue());
		if (wc.getDeletedUsers().containsKey(dn)) {
			wc.getDeletedUsers().remove(dn);
			wc.getModifiedUsers().put(dn, attrs);
		} else {
			wc.getModifiedUsers().put(dn, attrs);
			DirectoryUser newRole = newRole(dn, type, attrs);
			wc.getNewUsers().put(dn, newRole);
		}
		return getRole(name);
	}

	protected DirectoryUser newRole(LdapName dn, int type, Attributes attrs) {
		LdifUser newRole;
		BasicAttribute objClass = new BasicAttribute(objectClass.name());
		if (type == Role.USER) {
			String userObjClass = newUserObjectClass(dn);
			objClass.add(userObjClass);
			if (posixAccount.name().equals(userObjClass)) {
				objClass.add(inetOrgPerson.name());
				objClass.add(organizationalPerson.name());
				objClass.add(person.name());

				String username;
				try {
					username = dn.getRdn(dn.size() - 1).toAttributes()
							.get(uid.name()).get().toString();
				} catch (NamingException e) {
					throw new UserDirectoryException(
							"Cannot extract username from " + dn, e);
				}
				// TODO look for uid in attributes too?
				attrs.put(uidNumber.name(), new Long(max(uidNumber.name()) + 1));
				attrs.put(homeDirectory.name(), generateHomeDirectory(username));
				// TODO create user private group
				// NB: on RHEL, the 'users' group has gid 100
				attrs.put(gidNumber.name(), 100);
				// attrs.put(LdifName.loginShell.name(),"/sbin/nologin");
			} else if (inetOrgPerson.name().equals(userObjClass)) {
				objClass.add(organizationalPerson.name());
				objClass.add(person.name());
			} else if (organizationalPerson.name().equals(userObjClass)) {
				objClass.add(person.name());
			}
			objClass.add(top);
			attrs.put(objClass);
			newRole = new LdifUser(this, dn, attrs);
		} else if (type == Role.GROUP) {
			String groupObjClass = getGroupObjectClass();
			objClass.add(groupObjClass);
			objClass.add(LdifName.extensibleObject.name());
			attrs.put(gidNumber.name(), new Long(max(gidNumber.name()) + 1));
			objClass.add(top);
			attrs.put(objClass);
			newRole = new LdifGroup(this, dn, attrs);
		} else
			throw new UserDirectoryException("Unsupported type " + type);
		return newRole;
	}

	@Override
	public boolean removeRole(String name) {
		checkEdit();
		UserDirectoryWorkingCopy wc = getWorkingCopy();
		LdapName dn = toDn(name);
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
			group.getAttributes().get(getMemberAttributeId())
					.remove(dn.toString());
		}
		return actuallyDeleted;
	}

	// POSIX
	/** Generate path for a new user home */
	protected String generateHomeDirectory(String username) {
		String base = homeDirectoryBase;
		int atIndex = username.indexOf('@');
		if (atIndex > 0) {
			String domain = username.substring(0, atIndex);
			String name = username.substring(atIndex + 1);
			return base + '/' + firstCharsToPath(domain, 2) + '/' + domain
					+ '/' + firstCharsToPath(name, 2) + '/' + name;
		} else if (atIndex == 0 || atIndex == (username.length() - 1)) {
			throw new ArgeoException("Unsupported username " + username);
		} else {
			return base + '/' + firstCharsToPath(username, 2) + '/' + username;
		}
	}

	protected long max(String attr) {
		long max;
		try {
			List<DirectoryUser> users = doGetRoles(FrameworkUtil
					.createFilter("(" + attr + "=*)"));
			max = 1000;
			for (DirectoryUser user : users) {
				long uid = Long.parseLong(user.getAttributes().get(attr).get()
						.toString());
				if (uid > max)
					max = uid;
			}
		} catch (Exception e) {
			throw new UserDirectoryException("Cannot get max of " + attr, e);
		}
		return max;
	}

	/**
	 * Creates depth from a string (typically a username) by adding levels based
	 * on its first characters: "aBcD",2 => a/aB
	 */
	public static String firstCharsToPath(String str, Integer nbrOfChars) {
		if (str.length() < nbrOfChars)
			throw new ArgeoException("String " + str
					+ " length must be greater or equal than " + nbrOfChars);
		StringBuffer path = new StringBuffer("");
		StringBuffer curr = new StringBuffer("");
		for (int i = 0; i < nbrOfChars; i++) {
			curr.append(str.charAt(i));
			path.append(curr);
			if (i < nbrOfChars - 1)
				path.append('/');
		}
		return path.toString();
	}

	// TRANSACTION
	protected void prepare(UserDirectoryWorkingCopy wc) {

	}

	protected void commit(UserDirectoryWorkingCopy wc) {

	}

	protected void rollback(UserDirectoryWorkingCopy wc) {

	}

	// void clearEditingTransactionXid() {
	// editingTransactionXid = null;
	// }

	// UTILITIES
	protected LdapName toDn(String name) {
		try {
			return new LdapName(name);
		} catch (InvalidNameException e) {
			throw new UserDirectoryException("Badly formatted name", e);
		}
	}

	// GETTERS

	String getMemberAttributeId() {
		return memberAttributeId;
	}

	List<String> getCredentialAttributeIds() {
		return credentialAttributeIds;
	}

	protected URI getUri() {
		return uri;
	}

	protected List<String> getIndexedUserProperties() {
		return indexedUserProperties;
	}

	protected void setIndexedUserProperties(List<String> indexedUserProperties) {
		this.indexedUserProperties = indexedUserProperties;
	}

	private static boolean readOnlyDefault(URI uri) {
		if (uri == null)
			return true;
		if (uri.getScheme().equals("file")) {
			File file = new File(uri);
			if (file.exists())
				return !file.canWrite();
			else
				return !file.getParentFile().canWrite();
		}
		return true;
	}

	public boolean isReadOnly() {
		return readOnly;
	}

	UserAdmin getExternalRoles() {
		return externalRoles;
	}

	public String getBaseDn() {
		return baseDn;
	}

	/** dn can be null, in that case a default should be returned. */
	protected String getUserObjectClass() {
		return userObjectClass;
	}

	protected String newUserObjectClass(LdapName dn) {
		if (dn != null
				&& dn.getRdn(dn.size() - 1).toAttributes().get(uid.name()) != null)
			return posixAccount.name();
		else
			return getUserObjectClass();
	}

	protected String getGroupObjectClass() {
		return groupObjectClass;
	}

	public Dictionary<String, ?> getProperties() {
		return properties;
	}

	public void setExternalRoles(UserAdmin externalRoles) {
		this.externalRoles = externalRoles;
	}

	public void setTransactionManager(TransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}

	public WcXaResource getXaResource() {
		return xaResource;
	}

}
