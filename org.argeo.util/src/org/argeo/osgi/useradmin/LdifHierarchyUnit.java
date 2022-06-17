package org.argeo.osgi.useradmin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.naming.directory.Attributes;
import javax.naming.ldap.LdapName;

import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.useradmin.Role;

/** LDIF/LDAP based implementation of {@link HierarchyUnit}. */
class LdifHierarchyUnit implements HierarchyUnit {
	private final AbstractUserDirectory directory;

	private final LdapName dn;
	private final int type;
	private final Attributes attributes;

	HierarchyUnit parent;
	List<HierarchyUnit> children = new ArrayList<>();

	LdifHierarchyUnit(AbstractUserDirectory directory, LdapName dn, int type, Attributes attributes) {
		Objects.requireNonNull(directory);
		Objects.requireNonNull(dn);

		this.directory = directory;
		this.dn = dn;
		this.type = type;
		this.attributes = attributes;
	}

	@Override
	public int getHierarchyChildCount() {
		return children.size();
	}

	@Override
	public HierarchyUnit getParent() {
		return parent;
	}

	@Override
	public HierarchyUnit getHierarchyChild(int i) {
		return children.get(i);
	}

	@Override
	public int getHierarchyUnitType() {
		return type;
	}

	@Override
	public String getHierarchyUnitName() {
		String name = LdapNameUtils.getLastRdnAsString(dn);
		// TODO check ou, o, etc.
		return name;
	}

	public Attributes getAttributes() {
		return attributes;
	}

	@Override
	public String getBasePath() {
		return dn.toString();
	}

	@Override
	public List<? extends Role> getRoles(String filter, boolean deep) {
		try {
			return directory.getRoles(dn, filter, deep);
		} catch (InvalidSyntaxException e) {
			throw new IllegalArgumentException("Cannot filter " + filter + " " + dn, e);
		}
	}

	@Override
	public int hashCode() {
		return dn.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof LdifHierarchyUnit))
			return false;
		return ((LdifHierarchyUnit) obj).dn.equals(dn);
	}

	@Override
	public String toString() {
		return "Hierarchy Unit " + dn.toString();
	}

}
