package org.argeo.security.ui;

import org.argeo.security.ui.views.AdminLogView;
import org.argeo.security.ui.views.UserProfile;
import org.eclipse.ui.IFolderLayout;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;

/** Home perspective for the current user */
public class MaintenancePerspective implements IPerspectiveFactory {
	public final static String ID = SecurityUiPlugin.PLUGIN_ID
			+ ".adminMaintenancePerspective";

	public void createInitialLayout(IPageLayout layout) {
		String editorArea = layout.getEditorArea();
		layout.setEditorAreaVisible(true);
		layout.setFixed(false);

		IFolderLayout bottom = layout.createFolder("bottom",
				IPageLayout.BOTTOM, 0.50f, editorArea);
		bottom.addView(AdminLogView.ID);

		IFolderLayout left = layout.createFolder("left", IPageLayout.LEFT,
				0.30f, editorArea);
		left.addView(UserProfile.ID);
		// left.addView(RolesView.ID);

	}

}
