package org.argeo.util.directory.ldap;

import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;

/** Utilities to simplify using {@link LdapName}. */
public class LdapNameUtils {

	public static LdapName relativeName(LdapName prefix, LdapName dn) {
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

	public static LdapName getParent(LdapName dn) {
		try {
			LdapName parent = (LdapName) dn.clone();
			parent.remove(parent.size() - 1);
			return parent;
		} catch (InvalidNameException e) {
			throw new IllegalArgumentException("Cannot get parent of " + dn, e);
		}
	}

	public static Rdn getParentRdn(LdapName dn) {
		if (dn.size() < 2)
			throw new IllegalArgumentException(dn + " has no parent");
		Rdn parentRdn = dn.getRdn(dn.size() - 2);
		return parentRdn;
	}

	public static LdapName toLdapName(String distinguishedName) {
		try {
			return new LdapName(distinguishedName);
		} catch (InvalidNameException e) {
			throw new IllegalArgumentException("Cannot parse " + distinguishedName + " as LDAP name", e);
		}
	}

	public static Rdn getLastRdn(LdapName dn) {
		return dn.getRdn(dn.size() - 1);
	}

	public static String getLastRdnAsString(LdapName dn) {
		return getLastRdn(dn).toString();
	}

	public static String getLastRdnValue(LdapName dn) {
		return getLastRdn(dn).getValue().toString();
	}

	/** singleton */
	private LdapNameUtils() {

	}
}
