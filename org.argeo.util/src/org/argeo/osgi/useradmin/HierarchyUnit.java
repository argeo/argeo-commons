package org.argeo.osgi.useradmin;

import java.util.List;

import org.osgi.service.useradmin.Role;

/** A unit within the high-level organisational structure of a directory. */
public interface HierarchyUnit {
	String getHierarchyUnitName();

	HierarchyUnit getParent();

	Iterable<HierarchyUnit> getDirectHierachyUnits(boolean functionalOnly);

	boolean isFunctional();

	String getContext();

	List<? extends Role> getHierarchyUnitRoles(String filter, boolean deep);

	UserDirectory getDirectory();

//	Map<String,Object> getHierarchyProperties();
}
