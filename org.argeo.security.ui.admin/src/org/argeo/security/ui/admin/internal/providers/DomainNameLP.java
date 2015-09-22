package org.argeo.security.ui.admin.internal.providers;

import java.util.List;

import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;

import org.argeo.ArgeoException;
import org.argeo.security.ui.admin.internal.UserAdminConstants;
import org.osgi.service.useradmin.User;

/** Returns the human friendly domain name for the corresponding user. */
public class DomainNameLP extends UserAdminAbstractLP {
	private static final long serialVersionUID = 5256703081044911941L;

	@Override
	public String getText(User user) {
		String dn = (String) user.getProperties().get(KEY_DN);
		if (dn.endsWith(UserAdminConstants.SYSTEM_ROLE_BASE_DN))
			return "System roles";
		try {
			LdapName name;
			name = new LdapName(dn);
			List<Rdn> rdns = name.getRdns();
			return (String) rdns.get(1).getValue() + '.'
					+ (String) rdns.get(0).getValue();
		} catch (InvalidNameException e) {
			throw new ArgeoException("Unable to get domain name for " + dn, e);
		}
	}
}