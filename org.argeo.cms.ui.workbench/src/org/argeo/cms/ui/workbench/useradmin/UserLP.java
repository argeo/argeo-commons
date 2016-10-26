package org.argeo.cms.ui.workbench.useradmin;

import org.argeo.cms.ui.workbench.internal.useradmin.UsersImages;
import org.argeo.cms.util.UserAdminUtils;
import org.argeo.node.NodeConstants;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.osgi.service.useradmin.Role;
import org.osgi.service.useradmin.User;

/** Centralize label providers for the group table */
class UserLP extends ColumnLabelProvider {
	private static final long serialVersionUID = -4645930210988368571L;

	final static String COL_ICON = "colID.icon";
	final static String COL_DN = "colID.dn";
	final static String COL_DISPLAY_NAME = "colID.displayName";
	final static String COL_DOMAIN = "colID.domain";

	final String currType;

	// private Font italic;
	private Font bold;

	UserLP(String colId) {
		this.currType = colId;
	}

	@Override
	public Font getFont(Object element) {
		// Current user as bold
		if (UserAdminUtils.isCurrentUser(((User) element))) {
			if (bold == null)
				bold = JFaceResources.getFontRegistry().defaultFontDescriptor().setStyle(SWT.BOLD)
						.createFont(Display.getCurrent());
			return bold;
		}
		return null;
	}

	@Override
	public Image getImage(Object element) {
		if (COL_ICON.equals(currType)) {
			User user = (User) element;
			String dn = user.getName();
			if (dn.endsWith(NodeConstants.ROLES_BASEDN))
				return UsersImages.ICON_ROLE;
			else if (user.getType() == Role.GROUP)
				return UsersImages.ICON_GROUP;
			else
				return UsersImages.ICON_USER;
		} else
			return null;
	}

	@Override
	public String getText(Object element) {
		User user = (User) element;
		return getText(user);

	}

	public String getText(User user) {
		if (COL_DN.equals(currType))
			return user.getName();
		else if (COL_DISPLAY_NAME.equals(currType))
			return UserAdminUtils.getCommonName(user);
		else if (COL_DOMAIN.equals(currType))
			return UserAdminUtils.getDomainName(user);
		else
			return "";
	}
}
