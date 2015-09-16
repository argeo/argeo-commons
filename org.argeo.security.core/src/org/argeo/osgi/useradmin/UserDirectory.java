package org.argeo.osgi.useradmin;

import java.util.Dictionary;

import javax.transaction.TransactionManager;

import org.osgi.service.useradmin.UserAdmin;

/** Information about a user directory. */
public interface UserDirectory {
	public String getBaseDn();

	public void setExternalRoles(UserAdmin externalRoles);

	public Dictionary<String, ?> getProperties();

	// Transitional. In the future, more will be managed in OSGi.
	public void setTransactionManager(TransactionManager transactionManager);

	public void init();

	public void destroy();
}
