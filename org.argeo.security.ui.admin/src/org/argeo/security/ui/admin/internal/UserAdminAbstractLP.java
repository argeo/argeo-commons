package org.argeo.security.ui.admin.internal;

import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;

import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.widgets.Display;
import org.osgi.service.useradmin.User;

/**
 * Utility class that add font modifications to a column label provider
 * depending on the given user properties
 */
public abstract class UserAdminAbstractLP extends ColumnLabelProvider implements
		UserAdminConstants {
	private static final long serialVersionUID = 137336765024922368L;

	// private Font italic;
	private Font bold;

	@Override
	public Font getFont(Object element) {
		// Self as bold
		try {
			LdapName selfUserName = UiAdminUtils.getLdapName();
			String userName = UiAdminUtils.getProperty((User) element,
					UserAdminConstants.KEY_DN);
			LdapName userLdapName = new LdapName(userName);
			if (userLdapName.equals(selfUserName))
				return bold;
		} catch (InvalidNameException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// Disabled as Italic
		// Node userProfile = (Node) elem;
		// if (!userProfile.getProperty(ARGEO_ENABLED).getBoolean())
		// return italic;

		return null;
		// return super.getFont(element);
	}

	@Override
	public String getText(Object element) {
		User user = (User) element;
		return getText(user);
	}

	public void setDisplay(Display display) {
		// italic = JFaceResources.getFontRegistry().defaultFontDescriptor()
		// .setStyle(SWT.ITALIC).createFont(display);
		bold = JFaceResources.getFontRegistry().defaultFontDescriptor()
				.setStyle(SWT.BOLD).createFont(display);
	}

	public abstract String getText(User user);
}
