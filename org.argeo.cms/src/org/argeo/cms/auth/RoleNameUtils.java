package org.argeo.cms.auth;

public class RoleNameUtils {

	/*
	 * UTILITIES
	 */
	public final static String getLastRdnValue(String dn) {
		// we don't use LdapName for portability with Android
		// TODO make it more robust
		String[] parts = dn.split(",");
		String[] rdn = parts[0].split("=");
		return rdn[1];
	}

	public final static String getParent(String dn) {
		int index = dn.indexOf(',');
		return dn.substring(index + 1);
	}

	/** Up two levels. */
	public final static String getContext(String dn) {
		return getParent(getParent(dn));
	}
}
