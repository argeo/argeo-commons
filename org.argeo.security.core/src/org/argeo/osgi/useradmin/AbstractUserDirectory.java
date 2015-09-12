package org.argeo.osgi.useradmin;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.TransactionSynchronizationRegistry;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

import org.osgi.service.useradmin.Group;
import org.osgi.service.useradmin.Role;
import org.osgi.service.useradmin.User;
import org.osgi.service.useradmin.UserAdmin;

public abstract class AbstractUserDirectory implements UserAdmin {
	private boolean isReadOnly;
	private URI uri;

	private UserAdmin externalRoles;
	private List<String> indexedUserProperties = Arrays.asList(new String[] {
			"uid", "mail", "cn" });

	private String memberAttributeId = "member";
	private List<String> credentialAttributeIds = Arrays
			.asList(new String[] { "userpassword" });

	private TransactionSynchronizationRegistry syncRegistry;
	private Object editingTransactionKey = null;
	private TransactionManager transactionManager;
	private Transaction editingTransaction;

	public AbstractUserDirectory() {
	}

	public AbstractUserDirectory(URI uri, boolean isReadOnly) {
		this.uri = uri;
		this.isReadOnly = isReadOnly;
	}

	/** Returns the {@link Group}s this user is a direct member of. */
	protected abstract List<? extends Group> getDirectGroups(User user);

	public void init() {

	}

	public void destroy() {

	}

	boolean isEditing() {
		if (editingTransactionKey == null)
			return false;
		Object currentTrKey = syncRegistry.getTransactionKey();
		if (currentTrKey == null)
			return false;
		return editingTransactionKey.equals(currentTrKey);
	}

	void checkEdit() {
		Object currentTrKey = syncRegistry.getTransactionKey();
		if (currentTrKey == null)
			throw new UserDirectoryException(
					"A transaction needs to be active in order to edit");
		if (editingTransactionKey == null) {
			editingTransactionKey = currentTrKey;
			XAResource xaRes = getXAResource();
			if (xaRes != null)
				try {
					transactionManager.getTransaction().enlistResource(xaRes);
				} catch (Exception e) {
					throw new UserDirectoryException("Cannot enlist " + this, e);
				}
		} else {
			if (!editingTransactionKey.equals(currentTrKey))
				throw new UserDirectoryException("Transaction "
						+ editingTransactionKey + " already editing");
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

	public XAResource getXAResource() {
		return null;
	}

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

	public boolean isReadOnly() {
		return isReadOnly;
	}

	UserAdmin getExternalRoles() {
		return externalRoles;
	}

	public void setExternalRoles(UserAdmin externalRoles) {
		this.externalRoles = externalRoles;
	}

	public void setSyncRegistry(TransactionSynchronizationRegistry syncRegistry) {
		this.syncRegistry = syncRegistry;
	}

	public void setTransactionManager(TransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}

}
