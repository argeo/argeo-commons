package org.argeo.security.ui.views;

import org.argeo.security.ui.SecurityUiPlugin;
import org.argeo.security.ui.internal.CurrentUser;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.part.ViewPart;

public class UserProfile extends ViewPart {
	public static String ID = SecurityUiPlugin.PLUGIN_ID + ".userProfile";

	@Override
	public void createPartControl(Composite parent) {
		new Label(parent, SWT.NONE).setText(CurrentUser.getUsername());
	}

	@Override
	public void setFocus() {
	}

}
