package org.argeo.osgi.useradmin;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.naming.Context;

public enum LdapProperties {
	/** Base DN */
	baseDn("dc=example,dc=com"),

	/** URI of the underlying resource */
	uri("ldap://localhost:10389"),

	/** User objectClass */
	userObjectClass("inetOrgPerson"),

	/** Groups objectClass */
	groupObjectClass("groupOfNames"),

	/** Read-only source */
	readOnly(null);

	private static String PREFIX = "argeo.ldap.";

	/** The default value. */
	private Object def;

	LdapProperties(Object def) {
		this.def = def;
	}

	public Object getDefault() {
		return def;
	}

	public String getFullName() {
		return getPrefix() + name();
	}

	public String getPrefix() {
		return PREFIX;
	}

	public String getValue(Dictionary<String, ?> properties) {
		Object res = getRawValue(properties);
		if (res == null)
			return null;
		return res.toString();
	}

	@SuppressWarnings("unchecked")
	public <T> T getRawValue(Dictionary<String, ?> properties) {
		Object res = properties.get(getFullName());
		if (res == null)
			res = getDefault();
		return (T) res;
	}

	public static Dictionary<String, ?> uriAsProperties(String uriStr) {
		try {
			Hashtable<String, Object> res = new Hashtable<String, Object>();
			URI u = new URI(uriStr);
			String scheme = u.getScheme();
			String path = u.getPath();
			String bDn = path.substring(path.lastIndexOf('/') + 1,
					path.length());
			String principal = null;
			String credentials = null;
			if (scheme != null)
				if (scheme.equals("ldap") || scheme.equals("ldaps")) {
					// TODO additional checks
					String[] userInfo = u.getUserInfo().split(":");
					principal = userInfo.length > 0 ? userInfo[0] : null;
					credentials = userInfo.length > 1 ? userInfo[1] : null;
				} else if (scheme.equals("file")) {
					if (bDn.endsWith(".ldif")) {
						bDn = bDn.substring(0, bDn.length() - ".ldif".length());
					}
				} else
					throw new UserDirectoryException("Unsupported scheme "
							+ scheme);
			Map<String, List<String>> query = splitQuery(u.getQuery());
			for (String key : query.keySet()) {
				LdapProperties ldapProp = LdapProperties.valueOf(key);
				List<String> values = query.get(key);
				if (values.size() == 1) {
					res.put(ldapProp.getFullName(), values.get(0));
				} else {
					throw new UserDirectoryException(
							"Only single values are supported");
				}
			}
			res.put(baseDn.getFullName(), bDn);
			if (principal != null)
				res.put(Context.SECURITY_PRINCIPAL, principal);
			if (credentials != null)
				res.put(Context.SECURITY_CREDENTIALS, credentials);
			if (scheme != null) {
				URI bareUri = new URI(scheme, null, u.getHost(), u.getPort(),
						scheme.equals("file") ? u.getPath() : null, null, null);
				res.put(uri.getFullName(), bareUri.toString());
			}
			return res;
		} catch (Exception e) {
			throw new UserDirectoryException("Cannot convert " + uri
					+ " to properties", e);
		}
	}

	public static Map<String, List<String>> splitQuery(String query)
			throws UnsupportedEncodingException {
		final Map<String, List<String>> query_pairs = new LinkedHashMap<String, List<String>>();
		if (query == null)
			return query_pairs;
		final String[] pairs = query.split("&");
		for (String pair : pairs) {
			final int idx = pair.indexOf("=");
			final String key = idx > 0 ? URLDecoder.decode(
					pair.substring(0, idx), "UTF-8") : pair;
			if (!query_pairs.containsKey(key)) {
				query_pairs.put(key, new LinkedList<String>());
			}
			final String value = idx > 0 && pair.length() > idx + 1 ? URLDecoder
					.decode(pair.substring(idx + 1), "UTF-8") : null;
			query_pairs.get(key).add(value);
		}
		return query_pairs;
	}

	public static void main(String[] args) {
		System.out.println(uriAsProperties("ldap://"
				+ "uid=admin,ou=system:secret@localhost:10389"
				+ "/dc=example,dc=com"
				+ "?readOnly=false&userObjectClass=person"));
		System.out
				.println(uriAsProperties("file://some/dir/dc=example,dc=com.ldif"));
		System.out
				.println(uriAsProperties("/dc=example,dc=com.ldif?readOnly=true"));
	}
}
