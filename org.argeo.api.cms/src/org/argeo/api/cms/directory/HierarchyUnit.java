package org.argeo.api.cms.directory;

import java.util.Dictionary;
import java.util.Locale;

/** A unit within the high-level organisational structure of a directory. */
public interface HierarchyUnit {
	/** Name to use in paths. */
	String getHierarchyUnitName();

	/** Name to use in UI. */
	String getHierarchyUnitLabel(Locale locale);

	/**
	 * The parent {@link HierarchyUnit}, or <code>null</code> if a
	 * {@link Directory}.
	 */
	HierarchyUnit getParent();

	/** Direct children {@link HierarchyUnit}s. */
	Iterable<HierarchyUnit> getDirectHierarchyUnits(boolean functionalOnly);

	/**
	 * Whether this is an arbitrary named and placed {@link HierarchyUnit}.
	 * 
	 * @return <code>true</code> if functional, <code>false</code> is technical
	 *         (e.g. People, Groups, etc.)
	 */
	default boolean isFunctional() {
		return isType(Type.FUNCTIONAL);
	}

	boolean isType(Type type);

	/**
	 * The base of this organisational unit within the hierarchy. This would
	 * typically be an LDAP base DN.
	 */
	String getBase();

	/** The related {@link Directory}. */
	Directory getDirectory();

	/** Its metadata (typically LDAP attributes). */
	Dictionary<String, Object> getProperties();

	enum Type {
		PEOPLE, //
		GROUPS, //
		ROLES, //
		FUNCTIONAL;
	}
}
