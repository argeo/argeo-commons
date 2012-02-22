package org.argeo.security.ui;

import org.argeo.security.ui.views.LogView;
import org.argeo.security.ui.views.UserProfile;
import org.eclipse.ui.IFolderLayout;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;

/** Home perspective for the current user */
public class UserHomePerspective implements IPerspectiveFactory {
	public final static String ID = SecurityUiPlugin.PLUGIN_ID
			+ ".userHomePerspective";

	public void createInitialLayout(IPageLayout layout) {
		String editorArea = layout.getEditorArea();
		layout.setEditorAreaVisible(true);
		layout.setFixed(false);

		IFolderLayout left = layout.createFolder("left", IPageLayout.LEFT,
				0.30f, editorArea);
		left.addView(UserProfile.ID);
		left.addView(LogView.ID);
	}

}
