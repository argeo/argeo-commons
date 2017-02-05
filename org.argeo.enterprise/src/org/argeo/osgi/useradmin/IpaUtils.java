package org.argeo.osgi.useradmin;

import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;

import org.argeo.naming.LdapAttrs;

/** Free IPA specific conventions. */
public class IpaUtils {
	public final static String IPA_USER_BASE = "cn=users,cn=accounts";
	public final static String IPA_GROUP_BASE = "cn=groups,cn=accounts";
	public final static String IPA_SERVICE_BASE = "cn=services,cn=accounts";

	private final static String KRB_PRINCIPAL_NAME = LdapAttrs.krbPrincipalName.name().toLowerCase();

	public final static String IPA_USER_DIRECTORY_CONFIG = UserAdminConf.userBase + "=" + IPA_USER_BASE + "&"
			+ UserAdminConf.groupBase + "=" + IPA_GROUP_BASE + "&" + UserAdminConf.readOnly + "=true";

	static String domainToUserDirectoryConfigPath(String domain) {
		return domainToBaseDn(domain) + "?" + IPA_USER_DIRECTORY_CONFIG;
	}

	public static String domainToBaseDn(String domain) {
		String[] dcs = domain.split("\\.");
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < dcs.length; i++) {
			if (i != 0)
				sb.append(',');
			String dc = dcs[i];
			sb.append(LdapAttrs.dc.name()).append('=').append(dc.toLowerCase());
		}
		return sb.toString();
	}

	public static LdapName kerberosToDn(String kerberosName) {
		String[] kname = kerberosName.split("@");
		String username = kname[0];
		String baseDn = domainToBaseDn(kname[1]);
		String dn;
		if (!username.contains("/"))
			dn = LdapAttrs.uid + "=" + username + "," + IPA_USER_BASE + "," + baseDn;
		else
			dn = KRB_PRINCIPAL_NAME + "=" + kerberosName + "," + IPA_SERVICE_BASE + "," + baseDn;
		try {
			return new LdapName(dn);
		} catch (InvalidNameException e) {
			throw new IllegalArgumentException("Badly formatted name for " + kerberosName + ": " + dn);
		}
	}

	private IpaUtils() {

	}
}
