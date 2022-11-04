package org.argeo.util.directory.ldap;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.StringJoiner;

import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;

import org.argeo.util.directory.DirectoryConf;
import org.argeo.util.naming.LdapAttrs;
import org.argeo.util.naming.dns.DnsBrowser;

/** Free IPA specific conventions. */
public class IpaUtils {
	public final static String IPA_USER_BASE = "cn=users";
	public final static String IPA_GROUP_BASE = "cn=groups";
	public final static String IPA_ROLE_BASE = "cn=roles";
	public final static String IPA_SERVICE_BASE = "cn=services";

	public final static String IPA_ACCOUNTS_BASE = "cn=accounts";

	private final static String KRB_PRINCIPAL_NAME = LdapAttrs.krbPrincipalName.name().toLowerCase();

	public final static String IPA_USER_DIRECTORY_CONFIG = DirectoryConf.userBase + "=" + IPA_USER_BASE + "&"
			+ DirectoryConf.groupBase + "=" + IPA_GROUP_BASE + "&" + DirectoryConf.systemRoleBase + "=" + IPA_ROLE_BASE
			+ "&" + DirectoryConf.readOnly + "=true";

	@Deprecated
	static String domainToUserDirectoryConfigPath(String realm) {
		return domainToBaseDn(realm) + "?" + IPA_USER_DIRECTORY_CONFIG + "&" + DirectoryConf.realm.name() + "=" + realm;
	}

	public static void addIpaConfig(String realm, Dictionary<String, Object> properties) {
		properties.put(DirectoryConf.baseDn.name(), domainToBaseDn(realm));
		properties.put(DirectoryConf.realm.name(), realm);
		properties.put(DirectoryConf.userBase.name(), IPA_USER_BASE);
		properties.put(DirectoryConf.groupBase.name(), IPA_GROUP_BASE);
		properties.put(DirectoryConf.systemRoleBase.name(), IPA_ROLE_BASE);
		properties.put(DirectoryConf.readOnly.name(), Boolean.TRUE.toString());
	}

	public static String domainToBaseDn(String domain) {
		String[] dcs = domain.split("\\.");
		StringJoiner sj = new StringJoiner(",");
		for (int i = 0; i < dcs.length; i++) {
			String dc = dcs[i];
			sj.add(LdapAttrs.dc.name() + '=' + dc.toLowerCase());
		}
		return IPA_ACCOUNTS_BASE + ',' + sj.toString();
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
		} catch (IOException e) {
			throw new IllegalStateException("Cannot determine Kerberos domain from DNS", e);
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
			throw new IllegalStateException("No Kerberos domain available for " + uri);
		// TODO intergrate CA certificate in truststore
		// String schemeToUse = SCHEME_LDAPS;
		String schemeToUse = DirectoryConf.SCHEME_LDAP;
		List<String> ldapHosts;
		String ldapHostsStr = uri.getHost();
		if (ldapHostsStr == null || ldapHostsStr.trim().equals("")) {
			try (DnsBrowser dnsBrowser = new DnsBrowser()) {
				ldapHosts = dnsBrowser.getSrvRecordsAsHosts("_ldap._tcp." + kerberosRealm.toLowerCase(),
						schemeToUse.equals(DirectoryConf.SCHEME_LDAP) ? true : false);
				if (ldapHosts == null || ldapHosts.size() == 0) {
					throw new IllegalStateException("Cannot configure LDAP for IPA " + uri);
				} else {
					ldapHostsStr = ldapHosts.get(0);
				}
			} catch (IOException e) {
				throw new IllegalStateException("Cannot convert IPA uri " + uri, e);
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
			throw new IllegalStateException("Cannot convert IPA uri " + uri, e);
		}

		Hashtable<String, Object> res = new Hashtable<>();
		res.put(DirectoryConf.uri.name(), uriStr.toString());
		addIpaConfig(kerberosRealm, res);
		return res;
	}
}
