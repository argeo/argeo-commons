package org.argeo.security.ui.admin.internal.providers;

import org.argeo.security.ui.admin.internal.UiAdminUtils;
import org.osgi.service.useradmin.User;

/** The human friendly domain name for the corresponding user. */
public class DomainNameLP extends UserAdminAbstractLP {
	private static final long serialVersionUID = 5256703081044911941L;

	@Override
	public String getText(User user) {
		return UiAdminUtils.getDomainName(user);
	}
}