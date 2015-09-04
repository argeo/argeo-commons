package org.argeo.security.ui.admin.internal;

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
		String selfUserName = UiAdminUtils.getUsername();
		String userName = ((User) element).getName();
		if (userName.equals(selfUserName))
			return bold;

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
