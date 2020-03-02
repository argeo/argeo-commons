package org.argeo.cms.e4.users.providers;

import org.argeo.cms.auth.UserAdminUtils;
import org.argeo.naming.LdapAttrs;
import org.osgi.service.useradmin.User;

/** Simply declare a label provider that returns the Primary Mail of a user */
public class MailLP extends UserAdminAbstractLP {
	private static final long serialVersionUID = 8329764452141982707L;

	@Override
	public String getText(User user) {
		return UserAdminUtils.getProperty(user, LdapAttrs.mail.name());
	}
}
