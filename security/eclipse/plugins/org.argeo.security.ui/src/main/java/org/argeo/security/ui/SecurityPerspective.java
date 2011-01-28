package org.argeo.security.ui;

import org.eclipse.ui.IFolderLayout;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;

public class SecurityPerspective implements IPerspectiveFactory {

	public void createInitialLayout(IPageLayout layout) {
		String editorArea = layout.getEditorArea();
		layout.setEditorAreaVisible(true);
		layout.setFixed(false);

		IFolderLayout left = layout.createFolder("left", IPageLayout.LEFT,
				0.4f, editorArea);

		left.addView("org.argeo.security.ui.usersView");
		left.addView("org.argeo.security.ui.rolesView");
		//left.addView("org.argeo.security.ui.currentUserView");
	}

}
