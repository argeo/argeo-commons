package org.argeo.util.directory.ldap;

import java.util.Objects;

import javax.naming.directory.Attributes;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;

import org.argeo.util.directory.Directory;
import org.argeo.util.directory.HierarchyUnit;

/** LDIF/LDAP based implementation of {@link HierarchyUnit}. */
public class LdapHierarchyUnit implements HierarchyUnit {
	private final AbstractLdapDirectory directory;

	private final LdapName dn;
	private final boolean functional;
	private final Attributes attributes;

//	HierarchyUnit parent;
//	List<HierarchyUnit> children = new ArrayList<>();

	public LdapHierarchyUnit(AbstractLdapDirectory directory, LdapName dn, Attributes attributes) {
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
	public Directory getDirectory() {
		return directory;
	}

	@Override
	public int hashCode() {
		return dn.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof LdapHierarchyUnit))
			return false;
		return ((LdapHierarchyUnit) obj).dn.equals(dn);
	}

	@Override
	public String toString() {
		return "Hierarchy Unit " + dn.toString();
	}

}
