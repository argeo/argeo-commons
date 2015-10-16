package org.argeo.security.ui.admin.internal;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.List;

import javax.transaction.Status;
import javax.transaction.UserTransaction;

import org.argeo.ArgeoException;
import org.argeo.osgi.useradmin.UserAdminConf;
import org.osgi.framework.ServiceReference;
import org.osgi.service.useradmin.UserAdmin;
import org.osgi.service.useradmin.UserAdminEvent;
import org.osgi.service.useradmin.UserAdminListener;

/** Centralize interaction with the UserAdmin in this bundle */
public class UserAdminWrapper {
	// private Log log = LogFactory.getLog(UserAdminWrapper.class);

	private UserAdmin userAdmin;
	private ServiceReference<UserAdmin> userAdminServiceReference;
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

	public void setUserAdminServiceReference(
			ServiceReference<UserAdmin> userAdminServiceReference) {
		this.userAdminServiceReference = userAdminServiceReference;
	}

	public List<String> getKnownBaseDns(boolean onlyWritable) {
		List<String> dns = new ArrayList<String>();
		for (String uri : userAdminServiceReference.getPropertyKeys()) {
			if (!uri.startsWith("/"))
				continue;
			Dictionary<String, ?> props = UserAdminConf.uriAsProperties(uri);
			String readOnly = UserAdminConf.readOnly.getValue(props);
			String baseDn = UserAdminConf.baseDn.getValue(props);

			if (onlyWritable && "true".equals(readOnly))
				continue;
			if (baseDn.equalsIgnoreCase(UserAdminConstants.SYSTEM_ROLE_BASE_DN))
				continue;
			dns.add(baseDn);
		}
		return dns;
	}

	/* DEPENDENCY INJECTION */
	public void setUserAdmin(UserAdmin userAdmin) {
		this.userAdmin = userAdmin;
	}

	public void setUserTransaction(UserTransaction userTransaction) {
		this.userTransaction = userTransaction;
	}
}