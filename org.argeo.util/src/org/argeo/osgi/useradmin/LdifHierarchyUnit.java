package org.argeo.osgi.useradmin;

import java.util.List;
import java.util.Objects;

import javax.naming.directory.Attributes;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;

import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.useradmin.Role;

/** LDIF/LDAP based implementation of {@link HierarchyUnit}. */
class LdifHierarchyUnit implements HierarchyUnit {
	private final AbstractUserDirectory directory;

	private final LdapName dn;
	private final boolean functional;
	private final Attributes attributes;

//	HierarchyUnit parent;
//	List<HierarchyUnit> children = new ArrayList<>();

	LdifHierarchyUnit(AbstractUserDirectory directory, LdapName dn, Attributes attributes) {
		Objects.requireNonNull(directory);
		Objects.requireNonNull(dn);

		this.directory = directory;
		this.dn = dn;
		this.attributes = attributes;

		Rdn rdn = LdapNameUtils.getLastRdn(dn);
		functional = !(directory.getUserBaseRdn().equals(rdn) || directory.getGroupBaseRdn().equals(rdn)
				|| directory.getSystemRoleBaseRdn().equals(rdn));
	}

	@Override
	public HierarchyUnit getParent() {
		return directory.doGetHierarchyUnit(LdapNameUtils.getParent(dn));
	}

	@Override
	public Iterable<HierarchyUnit> getDirectHierachyUnits(boolean functionalOnly) {
//		List<HierarchyUnit> res = new ArrayList<>();
//		if (functionalOnly)
//			for (HierarchyUnit hu : children) {
//				if (hu.isFunctional())
//					res.add(hu);
//			}
//		else
//			res.addAll(children);
//		return Collections.unmodifiableList(res);
		return directory.doGetDirectHierarchyUnits(dn, functionalOnly);
	}

	@Override
	public boolean isFunctional() {
		return functional;
	}

	@Override
	public String getHierarchyUnitName() {
		String name = LdapNameUtils.getLastRdnValue(dn);
		// TODO check ou, o, etc.
		return name;
	}

	public Attributes getAttributes() {
		return attributes;
	}

	@Override
	public String getContext() {
		return dn.toString();
	}

	@Override
	public List<? extends Role> getHierarchyUnitRoles(String filter, boolean deep) {
		try {
			return directory.getRoles(dn, filter, deep);
		} catch (InvalidSyntaxException e) {
			throw new IllegalArgumentException("Cannot filter " + filter + " " + dn, e);
		}
	}

	@Override
	public UserDirectory getDirectory() {
		return directory;
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
