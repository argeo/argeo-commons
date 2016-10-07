package org.argeo.cms.ui.workbench.internal.useradmin;

import java.security.AccessController;
import java.util.List;

import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;
import javax.security.auth.Subject;
import javax.security.auth.x500.X500Principal;

import org.argeo.eclipse.ui.EclipseUiException;
import org.argeo.naming.LdapAttrs;
import org.argeo.node.NodeConstants;
import org.osgi.service.useradmin.Role;
import org.osgi.service.useradmin.User;

/**
 * Utility methods to manage user concepts in the ui.workbench bundle 
 * 
 * FIXME refactor amd centralise userAdminUtils
 */
@Deprecated
public class UsersUtils {

	public final static boolean isCurrentUser(User user) {
		String userName = getProperty(user, LdapAttrs.DN);
		try {
			LdapName selfUserName = getLdapName();
			LdapName userLdapName = new LdapName(userName);
			if (userLdapName.equals(selfUserName))
				return true;
			else
				return false;
		} catch (InvalidNameException e) {
			throw new EclipseUiException("User " + user
					+ " has an unvalid dn: " + userName, e);
		}
	}

	public final static LdapName getLdapName() {
		Subject subject = Subject.getSubject(AccessController.getContext());
		String name = subject.getPrincipals(X500Principal.class).iterator()
				.next().toString();
		LdapName dn;
		try {
			dn = new LdapName(name);
		} catch (InvalidNameException e) {
			throw new EclipseUiException("Invalid user dn " + name, e);
		}
		return dn;
	}

	public final static String getCommonName(User user) {
		return getProperty(user, LdapAttrs.cn.name());
	}

	/** Simply retrieves a display name of the relevant domain */
	public final static String getDomainName(User user) {
		String dn = (String) user.getProperties().get(LdapAttrs.DN);
		if (dn.endsWith(NodeConstants.ROLES_BASEDN))
			return "System roles";
		try {
			LdapName name;
			name = new LdapName(dn);
			List<Rdn> rdns = name.getRdns();
			String dname = null;
			int i = 0;
			loop: while (i < rdns.size()) {
				Rdn currrRdn = rdns.get(i);
				if (!"dc".equals(currrRdn.getType()))
					break loop;
				else {
					String currVal = (String) currrRdn.getValue();
					dname = dname == null ? currVal : currVal + "." + dname;
				}
				i++;
			}
			return dname;
		} catch (InvalidNameException e) {
			throw new EclipseUiException("Unable to get domain name for " + dn,
					e);
		}
	}

	public final static String getProperty(Role role, String key) {
		Object obj = role.getProperties().get(key);
		if (obj != null)
			return (String) obj;
		else
			return "";
	}
}
