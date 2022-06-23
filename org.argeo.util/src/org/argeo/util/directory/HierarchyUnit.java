package org.argeo.util.directory;

import java.util.Dictionary;

/** A unit within the high-level organisational structure of a directory. */
public interface HierarchyUnit {
	String getHierarchyUnitName();

	HierarchyUnit getParent();

	Iterable<HierarchyUnit> getDirectHierachyUnits(boolean functionalOnly);

	boolean isFunctional();

	String getContext();

	Directory getDirectory();

	Dictionary<String, Object> getProperties();
}
