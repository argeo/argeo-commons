package org.argeo.osgi.useradmin;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.naming.Context;
import javax.naming.NamingException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.argeo.naming.DnsBrowser;
import org.osgi.framework.Constants;

/** Properties used to configure user admins. */
public enum UserAdminConf {
	/** Base DN (cannot be configured externally) */
	baseDn("dc=example,dc=com"),

	/** URI of the underlying resource (cannot be configured externally) */
	uri("ldap://localhost:10389"),

	/** User objectClass */
	userObjectClass("inetOrgPerson"),

	/** Relative base DN for users */
	userBase("ou=People"),

	/** Groups objectClass */
	groupObjectClass("groupOfNames"),

	/** Relative base DN for users */
	groupBase("ou=Groups"),

	/** Read-only source */
	readOnly(null),

	/** Authentication realm */
	realm(null);

	public final static String FACTORY_PID = "org.argeo.osgi.useradmin.config";
	private final static Log log = LogFactory.getLog(UserAdminConf.class);

	/** The default value. */
	private Object def;

	UserAdminConf(Object def) {
		this.def = def;
	}

	public Object getDefault() {
		return def;
	}

	/**
	 * For use as Java property.
	 * 
	 * @deprecated use {@link #name()} instead
	 */
	@Deprecated
	public String property() {
		return name();
	}

	public String getValue(Dictionary<String, ?> properties) {
		Object res = getRawValue(properties);
		if (res == null)
			return null;
		return res.toString();
	}

	@SuppressWarnings("unchecked")
	public <T> T getRawValue(Dictionary<String, ?> properties) {
		Object res = properties.get(name());
		if (res == null)
			res = getDefault();
		return (T) res;
	}

	/** @deprecated use {@link #valueOf(String)} instead */
	@Deprecated
	public static UserAdminConf local(String property) {
		return UserAdminConf.valueOf(property);
	}

	/** Hides host and credentials. */
	public static URI propertiesAsUri(Dictionary<String, ?> properties) {
		StringBuilder query = new StringBuilder();

		boolean first = true;
		for (Enumeration<String> keys = properties.keys(); keys.hasMoreElements();) {
			String key = keys.nextElement();
			// TODO clarify which keys are relevant (list only the enum?)
			if (!key.equals("service.factoryPid") && !key.equals("cn") && !key.equals("dn")
					&& !key.equals(Constants.SERVICE_PID) && !key.startsWith("java") && !key.equals(baseDn.name())
					&& !key.equals(uri.name())) {
				if (first)
					first = false;
				else
					query.append('&');
				query.append(valueOf(key).name());
				query.append('=').append(properties.get(key).toString());
			}
		}

		String bDn = (String) properties.get(baseDn.name());
		try {
			return new URI(null, null, bDn != null ? '/' + bDn : null, query.length() != 0 ? query.toString() : null,
					null);
		} catch (URISyntaxException e) {
			throw new UserDirectoryException("Cannot create URI from properties", e);
		}
	}

	public static Dictionary<String, Object> uriAsProperties(String uriStr) {
		try {
			Hashtable<String, Object> res = new Hashtable<String, Object>();
			URI u = new URI(uriStr);
			String scheme = u.getScheme();
			if (scheme != null && scheme.equals("ipa")) {
				u = convertIpaConfig(u);
				scheme = u.getScheme();
			}
			String path = u.getPath();
			String bDn = path.substring(path.lastIndexOf('/') + 1, path.length());
			if (bDn.endsWith(".ldif"))
				bDn = bDn.substring(0, bDn.length() - ".ldif".length());

			String principal = null;
			String credentials = null;
			if (scheme != null)
				if (scheme.equals("ldap") || scheme.equals("ldaps")) {
					// TODO additional checks
					if (u.getUserInfo() != null) {
						String[] userInfo = u.getUserInfo().split(":");
						principal = userInfo.length > 0 ? userInfo[0] : null;
						credentials = userInfo.length > 1 ? userInfo[1] : null;
					}
				} else if (scheme.equals("file")) {
				} else if (scheme.equals("ipa")) {
				} else
					throw new UserDirectoryException("Unsupported scheme " + scheme);
			Map<String, List<String>> query = splitQuery(u.getQuery());
			for (String key : query.keySet()) {
				UserAdminConf ldapProp = UserAdminConf.valueOf(key);
				List<String> values = query.get(key);
				if (values.size() == 1) {
					res.put(ldapProp.name(), values.get(0));
				} else {
					throw new UserDirectoryException("Only single values are supported");
				}
			}
			res.put(baseDn.name(), bDn);
			if (principal != null)
				res.put(Context.SECURITY_PRINCIPAL, principal);
			if (credentials != null)
				res.put(Context.SECURITY_CREDENTIALS, credentials);
			if (scheme != null) {
				URI bareUri = new URI(scheme, null, u.getHost(), u.getPort(),
						scheme.equals("file") ? u.getPath() : null, null, null);
				res.put(uri.name(), bareUri.toString());
			}
			return res;
		} catch (Exception e) {
			throw new UserDirectoryException("Cannot convert " + uri + " to properties", e);
		}
	}

	private static URI convertIpaConfig(URI uri) {
		String path = uri.getPath();
		String kerberosRealm;
		if (path == null || path.length() <= 1) {
			kerberosRealm = kerberosDomainFromDns();
		} else {
			kerberosRealm = path.substring(1);
		}

		if (kerberosRealm == null)
			throw new UserDirectoryException("No Kerberos domain available for " + uri);
		try (DnsBrowser dnsBrowser = new DnsBrowser()) {
			String ldapHostsStr = uri.getHost();
			if (ldapHostsStr == null || ldapHostsStr.trim().equals("")) {
				List<String> ldapHosts = dnsBrowser.getSrvRecordsAsHosts("_ldap._tcp." + kerberosRealm.toLowerCase());
				if (ldapHosts == null || ldapHosts.size() == 0) {
					throw new UserDirectoryException("Cannot configure LDAP for IPA " + uri);
				} else {
					ldapHostsStr = ldapHosts.get(0);
				}
			}
			URI convertedUri = new URI(
					"ldap://" + ldapHostsStr + "/" + IpaUtils.domainToUserDirectoryConfigPath(kerberosRealm));
			if (log.isDebugEnabled())
				log.debug("Converted " + uri + " to " + convertedUri);
			return convertedUri;
		} catch (NamingException | IOException | URISyntaxException e) {
			throw new UserDirectoryException("cannot convert IPA uri " + uri, e);
		}
	}

	private static String kerberosDomainFromDns() {
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

	private static Map<String, List<String>> splitQuery(String query) throws UnsupportedEncodingException {
		final Map<String, List<String>> query_pairs = new LinkedHashMap<String, List<String>>();
		if (query == null)
			return query_pairs;
		final String[] pairs = query.split("&");
		for (String pair : pairs) {
			final int idx = pair.indexOf("=");
			final String key = idx > 0 ? URLDecoder.decode(pair.substring(0, idx), "UTF-8") : pair;
			if (!query_pairs.containsKey(key)) {
				query_pairs.put(key, new LinkedList<String>());
			}
			final String value = idx > 0 && pair.length() > idx + 1
					? URLDecoder.decode(pair.substring(idx + 1), "UTF-8") : null;
			query_pairs.get(key).add(value);
		}
		return query_pairs;
	}

	public static void main(String[] args) {
		Dictionary<String, ?> props = uriAsProperties("ldap://" + "uid=admin,ou=system:secret@localhost:10389"
				+ "/dc=example,dc=com" + "?readOnly=false&userObjectClass=person");
		System.out.println(props);
		System.out.println(propertiesAsUri(props));

		System.out.println(uriAsProperties("file://some/dir/dc=example,dc=com.ldif"));

		props = uriAsProperties(
				"/dc=example,dc=com.ldif?readOnly=true" + "&userBase=ou=CoWorkers,ou=People&groupBase=ou=Roles");
		System.out.println(props);
		System.out.println(propertiesAsUri(props));
	}
}
