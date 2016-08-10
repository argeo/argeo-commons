package org.argeo.security.ui.admin.internal.providers;

import org.argeo.cms.util.useradmin.UserAdminUtils;
import org.argeo.osgi.useradmin.LdifName;
import org.osgi.service.useradmin.User;

/** Simply declare a label provider that returns the Primary Mail of a user */
public class MailLP extends UserAdminAbstractLP {
	private static final long serialVersionUID = 8329764452141982707L;

	@Override
	public String getText(User user) {
		return UserAdminUtils.getProperty(user, LdifName.mail.name());
	}
}