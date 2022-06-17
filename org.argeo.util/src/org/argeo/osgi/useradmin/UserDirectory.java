package org.argeo.osgi.useradmin;

import java.util.Optional;

import org.argeo.osgi.transaction.WorkControl;
import org.osgi.service.useradmin.Role;

/** Information about a user directory. */
public interface UserDirectory extends HierarchyUnit {
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

	HierarchyUnit getHierarchyUnit(String path);

	HierarchyUnit getHierarchyUnit(Role role);

	@Deprecated
	void setTransactionControl(WorkControl transactionControl);
}
