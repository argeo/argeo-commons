package org.argeo.osgi.useradmin;

import java.util.Optional;

import org.argeo.osgi.transaction.WorkControl;

/** Information about a user directory. */
public interface UserDirectory {
	/**
	 * The base of the hierarchy defined by this directory. This could typically be
	 * an LDAP base DN.
	 */
	String getBasePath();

//	/** The base DN of all entries in this user directory */
//	LdapName getBaseDn();

//	/** The related {@link XAResource} */
//	XAResource getXaResource();

	boolean isReadOnly();

	boolean isDisabled();

	String getUserObjectClass();

	String getUserBase();

	String getGroupObjectClass();

	String getGroupBase();

	Optional<String> getRealm();

	@Deprecated
	void setTransactionControl(WorkControl transactionControl);
}
