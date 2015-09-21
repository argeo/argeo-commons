package org.argeo.osgi.useradmin;

import static org.argeo.osgi.useradmin.LdifName.inetOrgPerson;
import static org.argeo.osgi.useradmin.LdifName.objectClass;
import static org.argeo.osgi.useradmin.LdifName.organizationalPerson;
import static org.argeo.osgi.useradmin.LdifName.person;
import static org.argeo.osgi.useradmin.LdifName.top;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.naming.InvalidNameException;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.useradmin.Authorization;
import org.osgi.service.useradmin.Group;
import org.osgi.service.useradmin.Role;
import org.osgi.service.useradmin.User;
import org.osgi.service.useradmin.UserAdmin;

abstract class AbstractUserDirectory implements UserAdmin, UserDirectory {
	private final static Log log = LogFactory
			.getLog(AbstractUserDirectory.class);

	private Dictionary<String, ?> properties;
	private String baseDn = "dc=example,dc=com";
	private String userObjectClass;
	private String groupObjectClass;

	private boolean isReadOnly;
	private URI uri;

	private UserAdmin externalRoles;
	private List<String> indexedUserProperties = Arrays.asList(new String[] {
			LdifName.uid.name(), LdifName.mail.name(), LdifName.cn.name() });

	private String memberAttributeId = "member";
	private List<String> credentialAttributeIds = Arrays
			.asList(new String[] { LdifName.userpassword.name() });

	// private TransactionSynchronizationRegistry syncRegistry;
	// private Object editingTransactionKey = null;

	private TransactionManager transactionManager;
	private ThreadLocal<WorkingCopy> workingCopy = new ThreadLocal<AbstractUserDirectory.WorkingCopy>();
	private Xid editingTransactionXid = null;

	AbstractUserDirectory(Dictionary<String, ?> properties) {
		// TODO make a copy?
		this.properties = properties;

		String uriStr = UserAdminConf.uri.getValue(properties);
		if (uriStr == null)
			uri = null;
		else
			try {
				uri = new URI(uriStr);
			} catch (URISyntaxException e) {
				throw new UserDirectoryException("Badly formatted URI", e);
			}

		baseDn = UserAdminConf.baseDn.getValue(properties).toString();
		String isReadOnly = UserAdminConf.readOnly.getValue(properties);
		if (isReadOnly == null)
			this.isReadOnly = readOnlyDefault(uri);
		else
			this.isReadOnly = new Boolean(isReadOnly);

		this.userObjectClass = UserAdminConf.userObjectClass
				.getValue(properties);
		this.groupObjectClass = UserAdminConf.groupObjectClass
				.getValue(properties);
	}

	// public AbstractUserDirectory(URI uri, boolean isReadOnly) {
	// this.uri = uri;
	// this.isReadOnly = isReadOnly;
	// }

	/** Returns the {@link Group}s this user is a direct member of. */
	protected abstract List<? extends DirectoryGroup> getDirectGroups(User user);

	protected abstract Boolean daoHasRole(LdapName dn);

	protected abstract DirectoryUser daoGetRole(LdapName key);

	protected abstract List<DirectoryUser> doGetRoles(Filter f);

	public void init() {

	}

	public void destroy() {

	}

	boolean isEditing() {
		if (editingTransactionXid == null)
			return false;
		return workingCopy.get() != null;
		// Object currentTrKey = syncRegistry.getTransactionKey();
		// if (currentTrKey == null)
		// return false;
		// return editingTransactionKey.equals(currentTrKey);
	}

	protected WorkingCopy getWorkingCopy() {
		WorkingCopy wc = workingCopy.get();
		if (wc == null)
			return null;
		if (wc.xid == null) {
			workingCopy.set(null);
			return null;
		}
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
		if (editingTransactionXid == null) {
			WorkingCopy wc = new WorkingCopy();
			try {
				transaction.enlistResource(wc);
				editingTransactionXid = wc.getXid();
				workingCopy.set(wc);
			} catch (Exception e) {
				throw new UserDirectoryException("Cannot enlist " + wc, e);
			}
		} else {
			if (workingCopy.get() == null)
				throw new UserDirectoryException("Transaction "
						+ editingTransactionXid + " already editing");
			else if (!editingTransactionXid.equals(workingCopy.get().getXid()))
				throw new UserDirectoryException("Working copy Xid "
						+ workingCopy.get().getXid() + " inconsistent with"
						+ editingTransactionXid);
		}
	}

	List<Role> getAllRoles(User user) {
		List<Role> allRoles = new ArrayList<Role>();
		if (user != null) {
			collectRoles(user, allRoles);
			allRoles.add(user);
		} else
			collectAnonymousRoles(allRoles);
		return allRoles;
	}

	private void collectRoles(User user, List<Role> allRoles) {
		for (Group group : getDirectGroups(user)) {
			// TODO check for loops
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
		LdapName key = toDn(name);
		WorkingCopy wc = getWorkingCopy();
		DirectoryUser user = daoGetRole(key);
		if (wc != null) {
			if (user == null && wc.getNewUsers().containsKey(key))
				user = wc.getNewUsers().get(key);
			else if (wc.getDeletedUsers().containsKey(key))
				user = null;
		}
		return user;
	}

	@SuppressWarnings("unchecked")
	@Override
	public Role[] getRoles(String filter) throws InvalidSyntaxException {
		WorkingCopy wc = getWorkingCopy();
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
		return null;
	}

	protected void doGetUser(String key, String value,
			List<DirectoryUser> collectedUsers) {
		try {
			Filter f = FrameworkUtil.createFilter("(&(" + objectClass + "="
					+ getUserObjectClass() + ")(" + key + "=" + value + "))");
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
		WorkingCopy wc = getWorkingCopy();
		LdapName dn = toDn(name);
		if ((daoHasRole(dn) && !wc.getDeletedUsers().containsKey(dn))
				|| wc.getNewUsers().containsKey(dn))
			throw new UserDirectoryException("Already a role " + name);
		BasicAttributes attrs = new BasicAttributes();
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
			String userObjClass = getUserObjectClass();
			objClass.add(userObjClass);
			if (inetOrgPerson.name().equals(userObjClass)) {
				objClass.add(organizationalPerson.name());
				objClass.add(person.name());
			} else if (organizationalPerson.name().equals(userObjClass)) {
				objClass.add(person.name());
			}
			objClass.add(top);
			attrs.put(objClass);
			newRole = new LdifUser(this, dn, attrs);
		} else if (type == Role.GROUP) {
			objClass.add(getGroupObjectClass());
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
		WorkingCopy wc = getWorkingCopy();
		LdapName dn = toDn(name);
		if (!daoHasRole(dn) && !wc.getNewUsers().containsKey(dn))
			return false;
		DirectoryUser user = (DirectoryUser) getRole(name);
		wc.getDeletedUsers().put(dn, user);
		// FIXME clarify directgroups
		for (DirectoryGroup group : getDirectGroups(user)) {
			group.getAttributes().get(getMemberAttributeId())
					.remove(dn.toString());
		}
		return true;
	}

	// TRANSACTION
	protected void prepare(WorkingCopy wc) {

	}

	protected void commit(WorkingCopy wc) {

	}

	protected void rollback(WorkingCopy wc) {

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

	String getMemberAttributeId() {
		return memberAttributeId;
	}

	List<String> getCredentialAttributeIds() {
		return credentialAttributeIds;
	}

	protected URI getUri() {
		return uri;
	}

	protected void setUri(URI uri) {
		this.uri = uri;
	}

	protected List<String> getIndexedUserProperties() {
		return indexedUserProperties;
	}

	protected void setIndexedUserProperties(List<String> indexedUserProperties) {
		this.indexedUserProperties = indexedUserProperties;
	}

	protected void setReadOnly(boolean isReadOnly) {
		this.isReadOnly = isReadOnly;
	}

	private static boolean readOnlyDefault(URI uri) {
		if (uri == null)
			return true;
		if (uri.getScheme().equals("file")) {
			File file = new File(uri);
			return !file.canWrite();
		}
		return true;
	}

	public boolean isReadOnly() {
		return isReadOnly;
	}

	UserAdmin getExternalRoles() {
		return externalRoles;
	}

	public String getBaseDn() {
		return baseDn;
	}

	protected String getUserObjectClass() {
		return userObjectClass;
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

	//
	// XA RESOURCE
	//
	protected class WorkingCopy implements XAResource {
		private Xid xid;
		private int transactionTimeout = 0;

		private Map<LdapName, DirectoryUser> newUsers = new HashMap<LdapName, DirectoryUser>();
		private Map<LdapName, Attributes> modifiedUsers = new HashMap<LdapName, Attributes>();
		private Map<LdapName, DirectoryUser> deletedUsers = new HashMap<LdapName, DirectoryUser>();

		@Override
		public void start(Xid xid, int flags) throws XAException {
			if (editingTransactionXid != null)
				throw new UserDirectoryException("Transaction "
						+ editingTransactionXid + " already editing");
			this.xid = xid;
		}

		@Override
		public void end(Xid xid, int flags) throws XAException {
			checkXid(xid);

			// clean collections
			newUsers.clear();
			newUsers = null;
			modifiedUsers.clear();
			modifiedUsers = null;
			deletedUsers.clear();
			deletedUsers = null;

			// clean IDs
			this.xid = null;
			editingTransactionXid = null;
		}

		@Override
		public int prepare(Xid xid) throws XAException {
			checkXid(xid);
			if (noModifications())
				return XA_RDONLY;
			try {
				AbstractUserDirectory.this.prepare(this);
			} catch (Exception e) {
				log.error("Cannot prepare " + xid, e);
				throw new XAException(XAException.XA_RBOTHER);
			}
			return XA_OK;
		}

		@Override
		public void commit(Xid xid, boolean onePhase) throws XAException {
			checkXid(xid);
			if (noModifications())
				return;
			try {
				if (onePhase)
					AbstractUserDirectory.this.prepare(this);
				AbstractUserDirectory.this.commit(this);
			} catch (Exception e) {
				log.error("Cannot commit " + xid, e);
				throw new XAException(XAException.XA_RBOTHER);
			}
		}

		@Override
		public void rollback(Xid xid) throws XAException {
			checkXid(xid);
			try {
				AbstractUserDirectory.this.rollback(this);
			} catch (Exception e) {
				log.error("Cannot rollback " + xid, e);
				throw new XAException(XAException.XA_HEURMIX);
			}
		}

		@Override
		public void forget(Xid xid) throws XAException {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean isSameRM(XAResource xares) throws XAException {
			return xares == this;
		}

		@Override
		public Xid[] recover(int flag) throws XAException {
			throw new UnsupportedOperationException();
		}

		@Override
		public int getTransactionTimeout() throws XAException {
			return transactionTimeout;
		}

		@Override
		public boolean setTransactionTimeout(int seconds) throws XAException {
			transactionTimeout = seconds;
			return true;
		}

		private Xid getXid() {
			return xid;
		}

		private void checkXid(Xid xid) throws XAException {
			if (this.xid == null)
				throw new XAException(XAException.XAER_OUTSIDE);
			if (!this.xid.equals(xid))
				throw new XAException(XAException.XAER_NOTA);
		}

		@Override
		protected void finalize() throws Throwable {
			if (editingTransactionXid != null)
				log.warn("Editing transaction still referenced but no working copy "
						+ editingTransactionXid);
			editingTransactionXid = null;
		}

		public boolean noModifications() {
			return newUsers.size() == 0 && modifiedUsers.size() == 0
					&& deletedUsers.size() == 0;
		}

		public Attributes getAttributes(LdapName dn) {
			if (modifiedUsers.containsKey(dn))
				return modifiedUsers.get(dn);
			return null;
		}

		public void startEditing(DirectoryUser user) {
			LdapName dn = user.getDn();
			if (modifiedUsers.containsKey(dn))
				throw new UserDirectoryException("Already editing " + dn);
			modifiedUsers.put(dn, (Attributes) user.getAttributes().clone());
		}

		public Map<LdapName, DirectoryUser> getNewUsers() {
			return newUsers;
		}

		public Map<LdapName, DirectoryUser> getDeletedUsers() {
			return deletedUsers;
		}

		public Map<LdapName, Attributes> getModifiedUsers() {
			return modifiedUsers;
		}

	}
}
