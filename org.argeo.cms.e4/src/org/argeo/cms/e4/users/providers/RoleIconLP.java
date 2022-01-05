package org.argeo.cms.e4.users.providers;

import org.argeo.api.cms.CmsData;
import org.argeo.api.cms.CmsConstants;
import org.argeo.cms.auth.UserAdminUtils;
import org.argeo.cms.e4.users.SecurityAdminImages;
import org.argeo.util.naming.LdapAttrs;
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
		if (dn.endsWith(CmsConstants.ROLES_BASEDN))
			return SecurityAdminImages.ICON_ROLE;
		else if (user.getType() == Role.GROUP) {
			String businessCategory = UserAdminUtils.getProperty(user, LdapAttrs.businessCategory);
			if (businessCategory != null && businessCategory.equals(CmsData.WORKGROUP))
				return SecurityAdminImages.ICON_WORKGROUP;
			return SecurityAdminImages.ICON_GROUP;
		} else
			return SecurityAdminImages.ICON_USER;
	}
}
