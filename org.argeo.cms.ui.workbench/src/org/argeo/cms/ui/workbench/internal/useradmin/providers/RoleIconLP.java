package org.argeo.cms.ui.workbench.internal.useradmin.providers;

import org.argeo.cms.ui.workbench.internal.useradmin.SecurityAdminImages;
import org.argeo.node.NodeConstants;
import org.eclipse.swt.graphics.Image;
import org.osgi.service.useradmin.Role;
import org.osgi.service.useradmin.User;

/** Provide a bundle specific image depending on the current user type */
public class RoleIconLP extends UserAdminAbstractLP {
	private static final long serialVersionUID = 6550449442061090388L;

	@Override
	public String getText(User user) {
		return "";
	}

	@Override
	public Image getImage(Object element) {
		User user = (User) element;
		String dn = user.getName();
		if (dn.endsWith(NodeConstants.ROLES_BASEDN))
			return SecurityAdminImages.ICON_ROLE;
		else if (user.getType() == Role.GROUP)
			return SecurityAdminImages.ICON_GROUP;
		else
			return SecurityAdminImages.ICON_USER;
	}
}
