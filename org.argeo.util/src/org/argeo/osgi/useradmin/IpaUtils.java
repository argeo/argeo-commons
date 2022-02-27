package org.argeo.osgi.useradmin;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;

import javax.naming.InvalidNameException;
import javax.naming.NamingException;
import javax.naming.ldap.LdapName;

import org.argeo.util.naming.DnsBrowser;
import org.argeo.util.naming.LdapAttrs;

/** Free IPA specific conventions. */
public class IpaUtils {
	public final static String IPA_USER_BASE = "cn=users,cn=accounts";
	public final static String IPA_GROUP_BASE = "cn=groups,cn=accounts";
	public final static String IPA_SERVICE_BASE = "cn=services,cn=accounts";

	private final static String KRB_PRINCIPAL_NAME = LdapAttrs.krbPrincipalName.name().toLowerCase();

	public final static String IPA_USER_DIRECTORY_CONFIG = UserAdminConf.userBase + "=" + IPA_USER_BASE + "&"
			+ UserAdminConf.groupBase + "=" + IPA_GROUP_BASE + "&" + UserAdminConf.readOnly + "=true";

	@Deprecated
	static String domainToUserDirectoryConfigPath(String realm) {
		return domainToBaseDn(realm) + "?" + IPA_USER_DIRECTORY_CONFIG + "&" + UserAdminConf.realm.name() + "=" + realm;
	}

	public static void addIpaConfig(String realm, Dictionary<String, Object> properties) {
		properties.put(UserAdminConf.baseDn.name(), domainToBaseDn(realm));
		properties.put(UserAdminConf.realm.name(), realm);
		properties.put(UserAdminConf.userBase.name(), IPA_USER_BASE);
		properties.put(UserAdminConf.groupBase.name(), IPA_GROUP_BASE);
		properties.put(UserAdminConf.readOnly.name(), Boolean.TRUE.toString());
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

	public static String kerberosDomainFromDns() {
		String kerberosDomain;
		try (DnsBrowser dnsBrowser = new DnsBrowser()) {
			InetAddress localhost = InetAddress.getLocalHost();
			String hostname = localhost.getHostName();
			String dnsZone = hostname.substring(hostname.indexOf('.') + 1);
			kerberosDomain = dnsBrowser.getRecord("_kerberos." + dnsZone, "TXT");
			return kerberosDomain;
		} catch (Exception e) {
			throw new UserDirectoryException("Cannot determine Kerberos domain from DNS", e);
		}

	}

	public static Dictionary<String, Object> convertIpaUri(URI uri) {
		String path = uri.getPath();
		String kerberosRealm;
		if (path == null || path.length() <= 1) {
			kerberosRealm = kerberosDomainFromDns();
		} else {
			kerberosRealm = path.substring(1);
		}

		if (kerberosRealm == null)
			throw new UserDirectoryException("No Kerberos domain available for " + uri);
		// TODO intergrate CA certificate in truststore
		// String schemeToUse = SCHEME_LDAPS;
		String schemeToUse = UserAdminConf.SCHEME_LDAP;
		List<String> ldapHosts;
		String ldapHostsStr = uri.getHost();
		if (ldapHostsStr == null || ldapHostsStr.trim().equals("")) {
			try (DnsBrowser dnsBrowser = new DnsBrowser()) {
				ldapHosts = dnsBrowser.getSrvRecordsAsHosts("_ldap._tcp." + kerberosRealm.toLowerCase(),
						schemeToUse.equals(UserAdminConf.SCHEME_LDAP) ? true : false);
				if (ldapHosts == null || ldapHosts.size() == 0) {
					throw new UserDirectoryException("Cannot configure LDAP for IPA " + uri);
				} else {
					ldapHostsStr = ldapHosts.get(0);
				}
			} catch (NamingException | IOException e) {
				throw new UserDirectoryException("cannot convert IPA uri " + uri, e);
			}
		} else {
			ldapHosts = new ArrayList<>();
			ldapHosts.add(ldapHostsStr);
		}

		StringBuilder uriStr = new StringBuilder();
		try {
			for (String host : ldapHosts) {
				URI convertedUri = new URI(schemeToUse + "://" + host + "/");
				uriStr.append(convertedUri).append(' ');
			}
		} catch (URISyntaxException e) {
			throw new UserDirectoryException("cannot convert IPA uri " + uri, e);
		}

		Hashtable<String, Object> res = new Hashtable<>();
		res.put(UserAdminConf.uri.name(), uriStr.toString());
		addIpaConfig(kerberosRealm, res);
		return res;
	}
}
