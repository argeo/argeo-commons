package org.argeo.eclipse.ui.workbench.users.internal;

import java.security.AccessController;

import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.security.auth.Subject;
import javax.security.auth.x500.X500Principal;

import org.argeo.ArgeoException;
import org.argeo.osgi.useradmin.LdifName;
import org.osgi.service.useradmin.Role;
import org.osgi.service.useradmin.User;

/** Utility methods to manage user concepts in the ui.workbench bundle */
public class UsersUtils {

	public final static boolean isCurrentUser(User user) {
		String userName = getProperty(user, LdifName.dn.name());
		try {
			LdapName selfUserName = getLdapName();
			LdapName userLdapName = new LdapName(userName);
			if (userLdapName.equals(selfUserName))
				return true;
			else
				return false;
		} catch (InvalidNameException e) {
			throw new ArgeoException("User " + user + " has an unvalid dn: "
					+ userName, e);
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
			throw new ArgeoException("Invalid user dn " + name, e);
		}
		return dn;
	}

	public final static String getProperty(Role role, String key) {
		Object obj = role.getProperties().get(key);
		if (obj != null)
			return (String) obj;
		else
			return "";
	}

	/*
	 * INTERNAL METHODS: Below methods are meant to stay here and are not part
	 * of a potential generic backend to manage the useradmin
	 */
	public final static boolean notNull(String string) {
		if (string == null)
			return false;
		else
			return !"".equals(string.trim());
	}
}