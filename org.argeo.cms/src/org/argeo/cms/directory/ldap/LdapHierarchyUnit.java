package org.argeo.cms.directory.ldap;

import java.util.Locale;

import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;

import org.argeo.api.cms.directory.HierarchyUnit;

/** LDIF/LDAP based implementation of {@link HierarchyUnit}. */
public class LdapHierarchyUnit extends DefaultLdapEntry implements HierarchyUnit {
//	private final boolean functional;

	private final Type type;

	public LdapHierarchyUnit(AbstractLdapDirectory directory, LdapName dn) {
		super(directory, dn);

		Rdn rdn = LdapNameUtils.getLastRdn(dn);
		if (directory.getUserBaseRdn().equals(rdn))
			type = Type.PEOPLE;
		else if (directory.getGroupBaseRdn().equals(rdn))
			type = Type.GROUPS;
		else if (directory.getSystemRoleBaseRdn().equals(rdn))
			type = Type.ROLES;
		else
			type = Type.FUNCTIONAL;
//		functional = !(directory.getUserBaseRdn().equals(rdn) || directory.getGroupBaseRdn().equals(rdn)
//				|| directory.getSystemRoleBaseRdn().equals(rdn));
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
	public boolean isType(Type type) {
		return this.type.equals(type);
	}

	@Override
	public String getHierarchyUnitName() {
		String name = LdapNameUtils.getLastRdnValue(getDn());
		// TODO check ou, o, etc.
		return name;
	}

	@Override
	public String getHierarchyUnitLabel(Locale locale) {
		String key = LdapNameUtils.getLastRdn(getDn()).getType();
		Object value = LdapEntry.getLocalized(getProperties(), key, locale);
		if (value == null)
			value = getHierarchyUnitName();
		assert value != null;
		return value.toString();
	}

	@Override
	public String getBase() {
		return getDn().toString();
	}

	@Override
	public String toString() {
		return "Hierarchy Unit " + getDn().toString();
	}

}
