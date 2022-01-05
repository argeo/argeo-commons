package org.argeo.cms.e4.users.providers;

import org.argeo.cms.auth.UserAdminUtils;
import org.argeo.util.naming.LdapAttrs;
import org.osgi.service.useradmin.User;

/** Simply declare a label provider that returns the common name of a user */
public class CommonNameLP extends UserAdminAbstractLP {
	private static final long serialVersionUID = 5256703081044911941L;

	@Override
	public String getText(User user) {
		return UserAdminUtils.getProperty(user, LdapAttrs.cn.name());
	}

	@Override
	public String getToolTipText(Object element) {
		return UserAdminUtils.getProperty((User) element, LdapAttrs.DN);
	}

}
