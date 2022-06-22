package org.argeo.util.directory;

import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import org.argeo.util.directory.ldap.IpaUtils;
import org.argeo.util.naming.NamingUtils;

/** Properties used to configure user admins. */
public enum DirectoryConf {
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

	/** Relative base DN for users */
	systemRoleBase("ou=Roles"),

	/** Read-only source */
	readOnly(null),

	/** Disabled source */
	disabled(null),

	/** Authentication realm */
	realm(null),

	/** Override all passwords with this value (typically for testing purposes) */
	forcedPassword(null);

	public final static String FACTORY_PID = "org.argeo.osgi.useradmin.config";

	public final static String SCHEME_LDAP = "ldap";
	public final static String SCHEME_LDAPS = "ldaps";
	public final static String SCHEME_FILE = "file";
	public final static String SCHEME_OS = "os";
	public final static String SCHEME_IPA = "ipa";

	private final static String SECURITY_PRINCIPAL = "java.naming.security.principal";
	private final static String SECURITY_CREDENTIALS = "java.naming.security.credentials";

	/** The default value. */
	private Object def;

	DirectoryConf(Object def) {
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
	public static DirectoryConf local(String property) {
		return DirectoryConf.valueOf(property);
	}

	/** Hides host and credentials. */
	public static URI propertiesAsUri(Dictionary<String, ?> properties) {
		StringBuilder query = new StringBuilder();

		boolean first = true;
//		for (Enumeration<String> keys = properties.keys(); keys.hasMoreElements();) {
//			String key = keys.nextElement();
//			// TODO clarify which keys are relevant (list only the enum?)
//			if (!key.equals("service.factoryPid") && !key.equals("cn") && !key.equals("dn")
//					&& !key.equals(Constants.SERVICE_PID) && !key.startsWith("java") && !key.equals(baseDn.name())
//					&& !key.equals(uri.name()) && !key.equals(Constants.OBJECTCLASS)
//					&& !key.equals(Constants.SERVICE_ID) && !key.equals("bundle.id")) {
//				if (first)
//					first = false;
//				else
//					query.append('&');
//				query.append(valueOf(key).name());
//				query.append('=').append(properties.get(key).toString());
//			}
//		}

		keys: for (DirectoryConf key : DirectoryConf.values()) {
			if (key.equals(baseDn) || key.equals(uri))
				continue keys;
			Object value = properties.get(key.name());
			if (value == null)
				continue keys;
			if (first)
				first = false;
			else
				query.append('&');
			query.append(key.name());
			query.append('=').append(value.toString());

		}

		Object bDnObj = properties.get(baseDn.name());
		String bDn = bDnObj != null ? bDnObj.toString() : null;
		try {
			return new URI(null, null, bDn != null ? '/' + bDn : null, query.length() != 0 ? query.toString() : null,
					null);
		} catch (URISyntaxException e) {
			throw new IllegalArgumentException("Cannot create URI from properties", e);
		}
	}

	public static Dictionary<String, Object> uriAsProperties(String uriStr) {
		try {
			Hashtable<String, Object> res = new Hashtable<String, Object>();
			URI u = new URI(uriStr);
			String scheme = u.getScheme();
			if (scheme != null && scheme.equals(SCHEME_IPA)) {
				return IpaUtils.convertIpaUri(u);
//				scheme = u.getScheme();
			}
			String path = u.getPath();
			// base DN
			String bDn = path.substring(path.lastIndexOf('/') + 1, path.length());
			if (bDn.equals("") && SCHEME_OS.equals(scheme)) {
				bDn = getBaseDnFromHostname();
			}

			if (bDn.endsWith(".ldif"))
				bDn = bDn.substring(0, bDn.length() - ".ldif".length());

			// Normalize base DN as LDAP name
//			bDn = new LdapName(bDn).toString();

			String principal = null;
			String credentials = null;
			if (scheme != null)
				if (scheme.equals(SCHEME_LDAP) || scheme.equals(SCHEME_LDAPS)) {
					// TODO additional checks
					if (u.getUserInfo() != null) {
						String[] userInfo = u.getUserInfo().split(":");
						principal = userInfo.length > 0 ? userInfo[0] : null;
						credentials = userInfo.length > 1 ? userInfo[1] : null;
					}
				} else if (scheme.equals(SCHEME_FILE)) {
				} else if (scheme.equals(SCHEME_IPA)) {
				} else if (scheme.equals(SCHEME_OS)) {
				} else
					throw new IllegalArgumentException("Unsupported scheme " + scheme);
			Map<String, List<String>> query = NamingUtils.queryToMap(u);
			for (String key : query.keySet()) {
				DirectoryConf ldapProp = DirectoryConf.valueOf(key);
				List<String> values = query.get(key);
				if (values.size() == 1) {
					res.put(ldapProp.name(), values.get(0));
				} else {
					throw new IllegalArgumentException("Only single values are supported");
				}
			}
			res.put(baseDn.name(), bDn);
			if (SCHEME_OS.equals(scheme))
				res.put(readOnly.name(), "true");
			if (principal != null)
				res.put(SECURITY_PRINCIPAL, principal);
			if (credentials != null)
				res.put(SECURITY_CREDENTIALS, credentials);
			if (scheme != null) {// relative URIs are dealt with externally
				if (SCHEME_OS.equals(scheme)) {
					res.put(uri.name(), SCHEME_OS + ":///");
				} else {
					URI bareUri = new URI(scheme, null, u.getHost(), u.getPort(),
							scheme.equals(SCHEME_FILE) ? u.getPath() : null, null, null);
					res.put(uri.name(), bareUri.toString());
				}
			}
			return res;
		} catch (URISyntaxException e) {
			throw new IllegalArgumentException("Cannot convert " + uri + " to properties", e);
		}
	}

	private static String getBaseDnFromHostname() {
		String hostname;
		try {
			hostname = InetAddress.getLocalHost().getHostName();
		} catch (UnknownHostException e) {
			hostname = "localhost.localdomain";
		}
		int dotIdx = hostname.indexOf('.');
		if (dotIdx >= 0) {
			String domain = hostname.substring(dotIdx + 1, hostname.length());
			String bDn = ("." + domain).replaceAll("\\.", ",dc=");
			bDn = bDn.substring(1, bDn.length());
			return bDn;
		} else {
			return "dc=" + hostname;
		}
	}

	/**
	 * Hash the base DN in order to have a deterministic string to be used as a cn
	 * for the underlying user directory.
	 */
	public static String baseDnHash(Dictionary<String, Object> properties) {
		String bDn = (String) properties.get(baseDn.name());
		if (bDn == null)
			throw new IllegalStateException("No baseDn in " + properties);
		return DirectoryDigestUtils.sha1str(bDn);
	}
}
