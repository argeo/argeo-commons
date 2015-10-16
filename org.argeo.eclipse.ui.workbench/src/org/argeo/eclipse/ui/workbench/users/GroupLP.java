package org.argeo.eclipse.ui.workbench.users;

import java.util.List;

import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;

import org.argeo.ArgeoException;
import org.argeo.eclipse.ui.workbench.users.internal.UsersImages;
import org.argeo.eclipse.ui.workbench.users.internal.UsersUtils;
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
class GroupLP extends ColumnLabelProvider {
	private static final long serialVersionUID = -4645930210988368571L;

	// TODO this constant is defined in the CMS
	final static String ROLES_BASEDN = "ou=roles,ou=node";

	final static String COL_ICON = "colID.icon";
	final static String COL_DN = "colID.dn";
	final static String COL_DISPLAY_NAME = "colID.displayName";
	final static String COL_DOMAIN = "colID.domain";

	final String currType;

	// private Font italic;
	private Font bold;

	GroupLP(String colId) {
		this.currType = colId;
	}

	@Override
	public Font getFont(Object element) {
		// Self as bold
		try {
			LdapName selfUserName = UsersUtils.getLdapName();
			String userName = UsersUtils.getProperty((User) element,
					LdifName.dn.name());
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
			String dn = (String) user.getProperties().get(LdifName.dn.name());
			if (dn.endsWith(ROLES_BASEDN))
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
		else if (COL_DISPLAY_NAME.equals(currType)) {
			Object obj = user.getProperties().get(LdifName.cn.name());
			if (obj != null)
				return (String) obj;
			else
				return "";
		} else if (COL_DOMAIN.equals(currType)) {
			String dn = (String) user.getProperties().get(LdifName.dn.name());
			if (dn.endsWith(ROLES_BASEDN))
				return "System roles";
			try {
				LdapName name;
				name = new LdapName(dn);
				List<Rdn> rdns = name.getRdns();
				return (String) rdns.get(1).getValue() + '.'
						+ (String) rdns.get(0).getValue();
			} catch (InvalidNameException e) {
				throw new ArgeoException("Unable to get domain name for " + dn,
						e);
			}
		} else
			return "";
	}
}