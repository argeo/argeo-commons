package org.argeo.osgi.useradmin;

import java.util.List;
import java.util.Map;

import org.osgi.service.useradmin.Role;

/** A unit within the high-level organisational structure of a directory. */
public interface HierarchyUnit {
	final static int UNKOWN = 0;
	final static int ORGANIZATION = 1;
	final static int OU = 2;

	String getHierarchyUnitName();

	int getHierarchyChildCount();

	HierarchyUnit getParent();

	HierarchyUnit getHierarchyChild(int i);

	int getHierarchyUnitType();

	String getBasePath();

	List<? extends Role> getRoles(String filter, boolean deep);
	
//	Map<String,Object> getHierarchyProperties();
}
