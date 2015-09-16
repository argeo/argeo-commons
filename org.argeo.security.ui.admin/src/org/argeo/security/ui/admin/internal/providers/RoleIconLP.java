package org.argeo.security.ui.admin.internal.providers;

import org.argeo.security.ui.admin.SecurityAdminImages;
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
		if (user.getType() == Role.GROUP)
			return SecurityAdminImages.ICON_GROUP;
		else
			return SecurityAdminImages.ICON_USER;
	}
}