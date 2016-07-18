package org.argeo.eclipse.ui.workbench.users;

import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;

import org.argeo.ArgeoException;
import org.argeo.eclipse.ui.workbench.internal.users.UsersImages;
import org.argeo.eclipse.ui.workbench.internal.users.UsersUtils;
import org.argeo.osgi.useradmin.LdifName;
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
		// Self as bold
		try {
			LdapName selfUserName = UsersUtils.getLdapName();
			String userName = ((User) element).getName();
			LdapName userLdapName = new LdapName(userName);
			if (userLdapName.equals(selfUserName)) {
				if (bold == null)
					bold = JFaceResources.getFontRegistry()
							.defaultFontDescriptor().setStyle(SWT.BOLD)
							.createFont(Display.getCurrent());
				return bold;
			}
		} catch (InvalidNameException e) {
			throw new ArgeoException("cannot parse dn for " + element, e);
		}

		// Disabled as Italic
		// Node userProfile = (Node) elem;
		// if (!userProfile.getProperty(ARGEO_ENABLED).getBoolean())
		// return italic;

		return null;
		// return super.getFont(element);
	}

	@Override
	public Image getImage(Object element) {
		if (COL_ICON.equals(currType)) {
			User user = (User) element;
			String dn = user.getName();
			if (dn.endsWith(UsersUtils.ROLES_BASEDN))
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
			return UsersUtils.getCommonName(user);
		else if (COL_DOMAIN.equals(currType))
			return UsersUtils.getDomainName(user);
		else
			return "";
	}
}