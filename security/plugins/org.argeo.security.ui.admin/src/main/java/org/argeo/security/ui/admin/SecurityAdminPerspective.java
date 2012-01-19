package org.argeo.security.ui.admin;

import org.argeo.security.ui.admin.views.RolesView;
import org.argeo.security.ui.admin.views.UsersView;
import org.eclipse.ui.IFolderLayout;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;

public class SecurityAdminPerspective implements IPerspectiveFactory {
	public void createInitialLayout(IPageLayout layout) {
		String editorArea = layout.getEditorArea();
		layout.setEditorAreaVisible(true);
		layout.setFixed(false);

		IFolderLayout left = layout.createFolder("left", IPageLayout.LEFT,
				0.65f, editorArea);
		left.addView(UsersView.ID);
		left.addView(RolesView.ID);
	}

}
