package org.argeo.security.ui.admin.internal.providers;

import org.argeo.security.ui.admin.SecurityAdminImages;
import org.argeo.security.ui.admin.internal.UserAdminConstants;
import org.eclipse.swt.graphics.Image;
import org.osgi.service.useradmin.Role;
import org.osgi.service.useradmin.User;

public class RoleIconLP extends UserAdminAbstractLP {
	private static final long serialVersionUID = 6550449442061090388L;

	@Override
	public String getText(User user) {
		return "";
	}

	@Override
	public Image getImage(Object element) {
		User user = (User) element;
		String dn = (String) user.getProperties().get(KEY_DN);
		if (dn.endsWith(UserAdminConstants.SYSTEM_ROLE_BASE_DN))
			return SecurityAdminImages.ICON_ROLE;
		else if (user.getType() == Role.GROUP)
			return SecurityAdminImages.ICON_GROUP;
		else
			return SecurityAdminImages.ICON_USER;
	}
}