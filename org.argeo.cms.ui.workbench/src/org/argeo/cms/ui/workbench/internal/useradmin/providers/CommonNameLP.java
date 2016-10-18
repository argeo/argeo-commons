package org.argeo.cms.ui.workbench.internal.useradmin.providers;

import org.argeo.cms.util.useradmin.UserAdminUtils;
import org.argeo.naming.LdapAttrs;
import org.osgi.service.useradmin.User;

/** Simply declare a label provider that returns the common name of a user */
public class CommonNameLP extends UserAdminAbstractLP {
	private static final long serialVersionUID = 5256703081044911941L;

	@Override
	public String getText(User user) {
		return UserAdminUtils.getProperty(user, LdapAttrs.cn.name());
	}
}
