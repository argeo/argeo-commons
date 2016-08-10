package org.argeo.security.ui.admin.internal;

import java.util.ArrayList;
import java.util.List;

import javax.transaction.Status;
import javax.transaction.UserTransaction;

import org.argeo.ArgeoException;
import org.osgi.service.useradmin.UserAdminEvent;
import org.osgi.service.useradmin.UserAdminListener;

/** Centralize interaction with the UserAdmin in this bundle */
public class UserAdminWrapper extends
		org.argeo.security.ui.admin.internal.AbstractUserAdminWrapper {
	// private Log log = LogFactory.getLog(UserAdminWrapper.class);

	// Registered listeners
	List<UserAdminListener> listeners = new ArrayList<UserAdminListener>();

	/**
	 * Overwrite the normal begin transaction behaviour to also notify the UI.
	 * Must be called from the UI Thread.
	 */
	public UserTransaction beginTransactionIfNeeded() {
		try {
			UserTransaction userTransaction = getUserTransaction();
			if (userTransaction.getStatus() == Status.STATUS_NO_TRANSACTION) {
				userTransaction.begin();
				UiAdminUtils.notifyTransactionStateChange(userTransaction);
			}
			return userTransaction;
		} catch (Exception e) {
			throw new ArgeoException("Unable to begin transaction", e);
		}
	}

	// TODO implement safer mechanism
	public void addListener(UserAdminListener userAdminListener) {
		if (!listeners.contains(userAdminListener))
			listeners.add(userAdminListener);
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
}