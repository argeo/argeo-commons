package org.argeo.api.cms.directory;

/** Information about a user directory. */
public interface UserDirectory extends CmsDirectory {

	HierarchyUnit getHierarchyUnit(CmsRole role);

	Iterable<? extends CmsRole> getHierarchyUnitRoles(HierarchyUnit hierarchyUnit, String filter, boolean deep);

	String getRolePath(CmsRole role);

	String getRoleSimpleName(CmsRole role);

	CmsRole getRoleByPath(String path);
}
