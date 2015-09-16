package org.argeo.security.ui.admin.internal;

import java.util.ArrayList;
import java.util.List;

import javax.transaction.Status;
import javax.transaction.UserTransaction;

import org.argeo.ArgeoException;
import org.osgi.service.useradmin.UserAdmin;
import org.osgi.service.useradmin.UserAdminEvent;
import org.osgi.service.useradmin.UserAdminListener;

/** Simplifies the interaction with the UserAdmin in this bundle */
public class UserAdminWrapper {

	private UserAdmin userAdmin;
	private UserTransaction userTransaction;

	// Registered listeners
	List<UserAdminListener> listeners = new ArrayList<UserAdminListener>();

	// TODO implement safer mechanism
	public void addListener(UserAdminListener userAdminListener) {
		if (!listeners.contains(userAdminListener))
			listeners.add(userAdminListener);
	}

	/** Must be called from the UI Thread. */
	public void beginTransactionIfNeeded() {
		try {
			if (userTransaction.getStatus() == Status.STATUS_NO_TRANSACTION) {
				userTransaction.begin();
				UiAdminUtils.notifyTransactionStateChange(userTransaction);
			}
		} catch (Exception e) {
			throw new ArgeoException("Unable to begin transaction", e);
		}
	}

	// Expose this?
	public void removeListener(UserAdminListener userAdminListener) {
		if (listeners.contains(userAdminListener))
			listeners.remove(userAdminListener);
	}

	public void notifyListeners(UserAdminEvent event) {
		for (UserAdminListener listener : listeners)
			listener.roleChanged(event);
	}

	public UserAdmin getUserAdmin() {
		return userAdmin;
	}

	public UserTransaction getUserTransaction() {
		return userTransaction;
	}

	/* DEPENDENCY INJECTION */
	public void setUserAdmin(UserAdmin userAdmin) {
		this.userAdmin = userAdmin;
	}

	public void setUserTransaction(UserTransaction userTransaction) {
		this.userTransaction = userTransaction;
	}

}
