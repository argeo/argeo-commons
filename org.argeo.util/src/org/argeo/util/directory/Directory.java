package org.argeo.util.directory;

import java.util.Optional;

import org.argeo.util.transaction.WorkControl;

/** An information directory (typicylly LDAP). */
public interface Directory extends HierarchyUnit {
	String getName();

	/** Whether this directory is read only. */
	boolean isReadOnly();

	/** Whether this directory is disabled. */
	boolean isDisabled();

	/** The realm (typically Kerberos) of this directory. */
	Optional<String> getRealm();

	/** Sets the transaction control used by this directory when editing. */
	void setTransactionControl(WorkControl transactionControl);

	/*
	 * HIERARCHY
	 */

	/** The hierarchy unit at this path. */
	HierarchyUnit getHierarchyUnit(String path);

	/** Create a new hierarchy unit. */
	HierarchyUnit createHierarchyUnit(String path);
}
