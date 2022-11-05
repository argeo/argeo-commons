package org.argeo.cms.osgi.useradmin;

import org.argeo.api.cms.directory.Directory;
import org.argeo.api.cms.directory.HierarchyUnit;
import org.osgi.service.useradmin.Role;

/** Information about a user directory. */
public interface UserDirectory extends Directory {

	HierarchyUnit getHierarchyUnit(Role role);

	Iterable<? extends Role> getHierarchyUnitRoles(HierarchyUnit hierarchyUnit, String filter, boolean deep);

	String getRolePath(Role role);

	String getRoleSimpleName(Role role);

	Role getRoleByPath(String path);
}
