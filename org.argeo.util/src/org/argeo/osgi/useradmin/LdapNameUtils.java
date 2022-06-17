package org.argeo.osgi.useradmin;

import java.util.StringJoiner;

import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;

/** Utilities to simplify using {@link LdapName}. */
public class LdapNameUtils {

	public static String toRevertPath(String dn, String prefix) {
		if (!dn.endsWith(prefix))
			throw new IllegalArgumentException("Prefix " + prefix + " not consistent with " + dn);
		String relativeName = dn.substring(0, dn.length() - prefix.length() - 1);
		LdapName name = toLdapName(relativeName);
		StringJoiner path = new StringJoiner("/");
		for (int i = 0; i < name.size(); i++) {
			path.add(name.get(i));
		}
		return path.toString();
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
