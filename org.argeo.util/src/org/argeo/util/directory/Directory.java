package org.argeo.util.directory;

import java.util.Dictionary;
import java.util.Optional;

import org.argeo.util.transaction.WorkControl;

public interface Directory {
	/**
	 * The base of the hierarchy defined by this directory. This could typically be
	 * an LDAP base DN.
	 */
	String getContext();

	String getName();

	boolean isReadOnly();

	boolean isDisabled();

	Optional<String> getRealm();

	void setTransactionControl(WorkControl transactionControl);

	/*
	 * METADATA
	 */
	public Dictionary<String, Object> getProperties();

	/*
	 * HIERARCHY
	 */

	Iterable<HierarchyUnit> getDirectHierarchyUnits(boolean functionalOnly);

	HierarchyUnit getHierarchyUnit(String path);

}
