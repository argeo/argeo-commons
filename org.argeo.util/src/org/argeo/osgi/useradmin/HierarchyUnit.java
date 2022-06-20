package org.argeo.osgi.useradmin;

import java.util.List;

import org.osgi.service.useradmin.Role;

/** A unit within the high-level organisational structure of a directory. */
public interface HierarchyUnit {
	final static int UNKOWN = 0;
	final static int ORGANIZATION = 1;
	final static int OU = 2;

	String getHierarchyUnitName();

	HierarchyUnit getParent();

	Iterable<HierarchyUnit> getDirectHierachyUnits();

	int getHierarchyUnitType();

	String getBasePath();

	List<? extends Role> getHierarchyUnitRoles(String filter, boolean deep);

	UserDirectory getDirectory();

//	Map<String,Object> getHierarchyProperties();
}
