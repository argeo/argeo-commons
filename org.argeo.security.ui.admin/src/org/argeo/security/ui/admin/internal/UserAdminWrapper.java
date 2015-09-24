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

/** Simplifies the interaction with the UserAdmin in this bundle */
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

	/* DEPENDENCY INJECTION */
	public void setUserAdmin(UserAdmin userAdmin) {
		this.userAdmin = userAdmin;
	}

	public void setUserAdminServiceReference(
			ServiceReference<UserAdmin> userAdminServiceReference) {
		this.userAdminServiceReference = userAdminServiceReference;
		// for (String uri : userAdminServiceReference.getPropertyKeys()) {
		// if (!uri.startsWith("/"))
		// continue;
		// log.debug(uri);
		// Dictionary<String, ?> props = UserAdminConf.uriAsProperties(uri);
		// log.debug(props);
		// }
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
			dns.add(baseDn);
		}
		return dns;
	}

	// // Returns the human friendly domain name give a dn.
	// public String getDomainName(String dn) {
	// if (dn.endsWith("ou=roles, ou=node"))
	// return "System roles";
	// try {
	//
	// LdapName name;
	// name = new LdapName(dn);
	// List<Rdn> rdns = name.getRdns();
	//
	// String penultimate = (String) rdns.get(rdns.size() - 2).getValue();
	// String last = (String) rdns.get(rdns.size() - 1).getValue();
	// return (penultimate + '.' + last);
	// } catch (InvalidNameException e) {
	// throw new ArgeoException("Unable to get domain name for " + dn, e);
	// }
	// }

	public void setUserTransaction(UserTransaction userTransaction) {
		this.userTransaction = userTransaction;
	}
}