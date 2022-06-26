package org.argeo.util.directory.ldap;

import javax.naming.directory.Attributes;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;

import org.argeo.util.directory.HierarchyUnit;

/** LDIF/LDAP based implementation of {@link HierarchyUnit}. */
public class LdapHierarchyUnit extends DefaultLdapEntry implements HierarchyUnit {
	private final boolean functional;

	public LdapHierarchyUnit(AbstractLdapDirectory directory, LdapName dn, Attributes attributes) {
		super(directory, dn, attributes);

		Rdn rdn = LdapNameUtils.getLastRdn(dn);
		functional = !(directory.getUserBaseRdn().equals(rdn) || directory.getGroupBaseRdn().equals(rdn)
				|| directory.getSystemRoleBaseRdn().equals(rdn) );
	}

	@Override
	public HierarchyUnit getParent() {
		return getDirectoryDao().doGetHierarchyUnit(LdapNameUtils.getParent(getDn()));
	}

	@Override
	public Iterable<HierarchyUnit> getDirectHierarchyUnits(boolean functionalOnly) {
		return getDirectoryDao().doGetDirectHierarchyUnits(getDn(), functionalOnly);
	}

	@Override
	public boolean isFunctional() {
		return functional;
	}

	@Override
	public String getHierarchyUnitName() {
		String name = LdapNameUtils.getLastRdnValue(getDn());
		// TODO check ou, o, etc.
		return name;
	}

	@Override
	public String getContext() {
		return getDn().toString();
	}

	@Override
	public String toString() {
		return "Hierarchy Unit " + getDn().toString();
	}

}
