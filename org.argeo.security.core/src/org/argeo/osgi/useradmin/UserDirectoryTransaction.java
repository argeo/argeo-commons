package org.argeo.osgi.useradmin;

import javax.transaction.UserTransaction;

import org.osgi.service.useradmin.UserAdmin;

class UserDirectoryTransaction {
	static ThreadLocal<UserDirectoryTransaction> current = new ThreadLocal<UserDirectoryTransaction>();

	private UserAdmin userAdmin;

	private UserTransaction userTransaction;

	public UserDirectoryTransaction(UserAdmin userAdmin) {
		this.userAdmin = userAdmin;
		if (current.get() != null)
			throw new UserDirectoryException("Transaction " + current.get()
					+ " already active.");
		current.set(this);
	}

	public void setUserTransaction(UserTransaction userTransaction) {
		this.userTransaction = userTransaction;
	}

}
