package org.argeo.cms.e4.users.providers;

import org.argeo.cms.auth.UserAdminUtils;
import org.osgi.service.useradmin.User;

/** The human friendly domain name for the corresponding user. */
public class DomainNameLP extends UserAdminAbstractLP {
	private static final long serialVersionUID = 5256703081044911941L;

	@Override
	public String getText(User user) {
		return UserAdminUtils.getDomainName(user);
	}
}
