package org.argeo.cms.auth;

import static org.argeo.api.acr.RuntimeNamespaceContext.getNamespaceContext;

import javax.xml.namespace.QName;

import org.argeo.api.acr.ArgeoNamespace;
import org.argeo.api.acr.NamespaceUtils;
import org.argeo.cms.directory.ldap.LdapNameUtils;

/** Simplifies analysis of system roles. */
public class RoleNameUtils {
	public static String getLastRdnValue(String dn) {
		return LdapNameUtils.getLastRdnValue(dn);
//		// we don't use LdapName for portability with Android
//		// TODO make it more robust
//		String[] parts = dn.split(",");
//		String[] rdn = parts[0].split("=");
//		return rdn[1];
	}

	public static QName getLastRdnAsName(String dn) {
		String cn = getLastRdnValue(dn);
		QName roleName = NamespaceUtils.parsePrefixedName(getNamespaceContext(), cn);
		return roleName;
	}

	public static boolean isSystemRole(QName roleName) {
		return roleName.getNamespaceURI().equals(ArgeoNamespace.ROLE_NAMESPACE_URI);
	}

	public static String getParent(String dn) {
		int index = dn.indexOf(',');
		return dn.substring(index + 1);
	}

	/** Up two levels. */
	public static String getContext(String dn) {
		return getParent(getParent(dn));
	}
}
