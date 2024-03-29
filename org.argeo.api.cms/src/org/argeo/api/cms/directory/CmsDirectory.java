package org.argeo.api.cms.directory;

import java.util.Optional;

import org.argeo.api.cms.transaction.WorkControl;

/** An information directory (typically LDAP). */
public interface CmsDirectory extends HierarchyUnit {
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
