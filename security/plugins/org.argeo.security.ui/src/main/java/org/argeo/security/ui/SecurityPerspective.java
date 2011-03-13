package org.argeo.security.ui;

import org.eclipse.ui.IFolderLayout;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;

public class SecurityPerspective implements IPerspectiveFactory {
	private String adminRole = "ROLE_ADMIN";

	public void createInitialLayout(IPageLayout layout) {
//		if (!CurrentUser.roles().contains(adminRole)) {
//			MessageDialog
//					.openError(Display.getCurrent().getActiveShell(),
//							"Forbidden",
//							"You are not allowed to access this resource.");
//			return;
//		}

		String editorArea = layout.getEditorArea();
		layout.setEditorAreaVisible(true);
		layout.setFixed(false);

		IFolderLayout left = layout.createFolder("left", IPageLayout.LEFT,
				0.4f, editorArea);

		left.addView("org.argeo.security.ui.adminUsersView");
		left.addView("org.argeo.security.ui.adminRolesView");
	}

	public void setAdminRole(String adminRole) {
		this.adminRole = adminRole;
	}

}
