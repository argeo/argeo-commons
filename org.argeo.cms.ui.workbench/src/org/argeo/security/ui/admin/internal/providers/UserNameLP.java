package org.argeo.security.ui.admin.internal.providers;

import org.osgi.service.useradmin.User;

/** Simply declare a label provider that returns the username of a user */
public class UserNameLP extends UserAdminAbstractLP {
	private static final long serialVersionUID = 6550449442061090388L;

	@Override
	public String getText(User user) {
		return user.getName();
	}
}
