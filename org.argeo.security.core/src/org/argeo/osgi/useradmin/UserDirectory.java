package org.argeo.osgi.useradmin;

import java.util.Dictionary;

import javax.transaction.TransactionManager;
import javax.transaction.xa.XAResource;

import org.osgi.service.useradmin.UserAdmin;

/** Information about a user directory. */
public interface UserDirectory {
	public String getBaseDn();

	public void setExternalRoles(UserAdmin externalRoles);

	/** Keys listed and described in {@link UserAdminConf}. */
	public Dictionary<String, Object> getProperties();

	// Transitional. In the future, more will be managed in OSGi.
	public void setTransactionManager(TransactionManager transactionManager);

	public void init();

	public void destroy();

	public XAResource getXaResource();
}
