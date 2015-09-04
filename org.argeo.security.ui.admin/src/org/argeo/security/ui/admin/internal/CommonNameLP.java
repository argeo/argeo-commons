package org.argeo.security.ui.admin.internal;

import org.osgi.service.useradmin.User;

/** Simply declare a label provider that returns the common name of a user */
public class CommonNameLP extends UserAdminAbstractLP {
	private static final long serialVersionUID = 5256703081044911941L;

	@Override
	public String getText(User user) {
		Object obj = user.getProperties().get(KEY_CN);
		if (obj != null)
			return (String) obj;
		else
			return "";
	}
}