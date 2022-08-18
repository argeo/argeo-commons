package org.argeo.util.directory;

import java.util.Dictionary;

/** A unit within the high-level organisational structure of a directory. */
public interface HierarchyUnit {
	String getHierarchyUnitName();

	HierarchyUnit getParent();

	Iterable<HierarchyUnit> getDirectHierarchyUnits(boolean functionalOnly);

	boolean isFunctional();

	/**
	 * The base of this organisational unit within the hierarchy. This would
	 * typically be an LDAP base DN.
	 */
	String getBase();

	Directory getDirectory();

	Dictionary<String, Object> getProperties();
}
