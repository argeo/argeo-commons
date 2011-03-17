package org.argeo.security.ui.admin;

import org.eclipse.ui.IFolderLayout;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;

public class SecurityAdminPerspective implements IPerspectiveFactory {
	public void createInitialLayout(IPageLayout layout) {
		String editorArea = layout.getEditorArea();
		layout.setEditorAreaVisible(true);
		layout.setFixed(false);

		IFolderLayout left = layout.createFolder("left", IPageLayout.LEFT,
				0.4f, editorArea);
		left.addView("org.argeo.security.ui.admin.adminUsersView");
		left.addView("org.argeo.security.ui.admin.adminRolesView");
	}

}
