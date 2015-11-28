package org.argeo.security.ui.admin.internal.providers;

import org.argeo.osgi.useradmin.LdifName;
import org.argeo.security.ui.admin.internal.UiAdminUtils;
import org.osgi.service.useradmin.User;

/** Simply declare a label provider that returns the common name of a user */
public class CommonNameLP extends UserAdminAbstractLP {
	private static final long serialVersionUID = 5256703081044911941L;

	@Override
	public String getText(User user) {
		return UiAdminUtils.getProperty(user, LdifName.cn.name());
	}
}