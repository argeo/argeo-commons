package org.argeo.osgi.useradmin;

import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;

/** Utilities to simplify using {@link LdapName}. */
class LdapNameUtils {

	static LdapName relativeName(LdapName prefix, LdapName dn) {
		try {
			if (!dn.startsWith(prefix))
				throw new IllegalArgumentException("Prefix " + prefix + " not consistent with " + dn);
			LdapName res = (LdapName) dn.clone();
			for (int i = 0; i < prefix.size(); i++) {
				res.remove(0);
			}
			return res;
		} catch (InvalidNameException e) {
			throw new IllegalStateException("Cannot find realtive name", e);
		}
	}

	static LdapName getParent(LdapName dn) {
		try {
			LdapName parent = (LdapName) dn.clone();
			parent.remove(parent.size() - 1);
			return parent;
		} catch (InvalidNameException e) {
			throw new IllegalArgumentException("Cannot get parent of " + dn, e);
		}
	}

	static Rdn getParentRdn(LdapName dn) {
		if (dn.size() < 2)
			throw new IllegalArgumentException(dn + " has no parent");
		Rdn parentRdn = dn.getRdn(dn.size() - 2);
		return parentRdn;
	}

	static LdapName toLdapName(String distinguishedName) {
		try {
			return new LdapName(distinguishedName);
		} catch (InvalidNameException e) {
			throw new IllegalArgumentException("Cannot parse " + distinguishedName + " as LDAP name", e);
		}
	}

	static Rdn getLastRdn(LdapName dn) {
		return dn.getRdn(dn.size() - 1);
	}

	static String getLastRdnAsString(LdapName dn) {
		return getLastRdn(dn).toString();
	}

	static String getLastRdnValue(LdapName dn) {
		return getLastRdn(dn).getValue().toString();
	}

	/** singleton */
	private LdapNameUtils() {

	}
}
