package org.argeo.osgi.useradmin;

import static org.argeo.naming.LdapAttrs.objectClass;
import static org.argeo.naming.LdapObjs.inetOrgPerson;
import static org.argeo.naming.LdapObjs.organizationalPerson;
import static org.argeo.naming.LdapObjs.person;
import static org.argeo.naming.LdapObjs.top;

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
import org.argeo.naming.LdapAttrs;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.useradmin.Authorization;
import org.osgi.service.useradmin.Role;
import org.osgi.service.useradmin.User;
import org.osgi.service.useradmin.UserAdmin;

/** Base class for a {@link UserDirectory}. */
public abstract class AbstractUserDirectory implements UserAdmin, UserDirectory {
	private final static Log log = LogFactory.getLog(AbstractUserDirectory.class);

	private final Hashtable<String, Object> properties;
	private final LdapName baseDn;
	private final String userObjectClass, userBase, groupObjectClass, groupBase;

	private final boolean readOnly;
	private final URI uri;

	private UserAdmin externalRoles;
	private List<String> indexedUserProperties = Arrays
			.asList(new String[] { LdapAttrs.uid.name(), LdapAttrs.mail.name(), LdapAttrs.cn.name() });

	private String memberAttributeId = "member";
	private List<String> credentialAttributeIds = Arrays.asList(new String[] { LdapAttrs.userPassword.name() });

	// JTA
	private TransactionManager transactionManager;
	private WcXaResource xaResource = new WcXaResource(this);

	public AbstractUserDirectory(Dictionary<String, ?> props) {
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
				throw new UserDirectoryException("Badly formatted URI " + uriStr, e);
			}

		try {
			baseDn = new LdapName(UserAdminConf.baseDn.getValue(properties));
		} catch (InvalidNameException e) {
			throw new UserDirectoryException("Badly formated base DN " + UserAdminConf.baseDn.getValue(properties), e);
		}
		String readOnlyStr = UserAdminConf.readOnly.getValue(properties);
		if (readOnlyStr == null) {
			readOnly = readOnlyDefault(uri);
			properties.put(UserAdminConf.readOnly.name(), Boolean.toString(readOnly));
		} else
			readOnly = new Boolean(readOnlyStr);

		userObjectClass = UserAdminConf.userObjectClass.getValue(properties);
		userBase = UserAdminConf.userBase.getValue(properties);
		groupObjectClass = UserAdminConf.groupObjectClass.getValue(properties);
		groupBase = UserAdminConf.groupBase.getValue(properties);
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
		Transaction transaction;
		try {
			transaction = transactionManager.getTransaction();
		} catch (SystemException e) {
			throw new UserDirectoryException("Cannot get transaction", e);
		}
		if (transaction == null)
			throw new UserDirectoryException("A transaction needs to be active in order to edit");
		if (xaResource.wc() == null) {
			try {
				transaction.enlistResource(xaResource);
			} catch (Exception e) {
				throw new UserDirectoryException("Cannot enlist " + xaResource, e);
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
		List<DirectoryUser> collectedUsers = new ArrayList<DirectoryUser>(getIndexedUserProperties().size());
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
			log.warn(collectedUsers.size() + " users for " + (key != null ? key + "=" : "") + value);
		return null;
	}

	protected void doGetUser(String key, String value, List<DirectoryUser> collectedUsers) {
		try {
			Filter f = FrameworkUtil.createFilter("(" + key + "=" + value + ")");
			List<DirectoryUser> users = doGetRoles(f);
			collectedUsers.addAll(users);
		} catch (InvalidSyntaxException e) {
			throw new UserDirectoryException("Cannot get user with " + key + "=" + value, e);
		}
	}

	@Override
	public Authorization getAuthorization(User user) {
		return new LdifAuthorization((DirectoryUser) user, getAllRoles((DirectoryUser) user));
	}

	@Override
	public Role createRole(String name, int type) {
		checkEdit();
		UserDirectoryWorkingCopy wc = getWorkingCopy();
		LdapName dn = toDn(name);
		if ((daoHasRole(dn) && !wc.getDeletedUsers().containsKey(dn)) || wc.getNewUsers().containsKey(dn))
			throw new UserDirectoryException("Already a role " + name);
		BasicAttributes attrs = new BasicAttributes(true);
		// attrs.put(LdifName.dn.name(), dn.toString());
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
			if (inetOrgPerson.name().equals(userObjClass)) {
				objClass.add(organizationalPerson.name());
				objClass.add(person.name());
			} else if (organizationalPerson.name().equals(userObjClass)) {
				objClass.add(person.name());
			}
			objClass.add(top.name());
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

	// UTILITIES
	protected LdapName toDn(String name) {
		try {
			return new LdapName(name);
		} catch (InvalidNameException e) {
			throw new UserDirectoryException("Badly formatted name", e);
		}
	}

	// GETTERS
	protected String getMemberAttributeId() {
		return memberAttributeId;
	}

	protected List<String> getCredentialAttributeIds() {
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

	protected UserAdmin getExternalRoles() {
		return externalRoles;
	}

	public LdapName getBaseDn() {
		// always clone so that the property is not modified by reference
		return (LdapName) baseDn.clone();
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

	public Dictionary<String, Object> getProperties() {
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
