package org.argeo.security.ui;

import org.eclipse.ui.IFolderLayout;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;

public class SecurityPerspective implements IPerspectiveFactory {

	public void createInitialLayout(IPageLayout layout) {
		String editorArea = layout.getEditorArea();
		layout.setEditorAreaVisible(false);
		layout.setFixed(false);

		IFolderLayout main = layout.createFolder("main", IPageLayout.RIGHT,
				0.3f, editorArea);
		IFolderLayout left = layout.createFolder("left", IPageLayout.LEFT,
				0.3f, editorArea);

		left.addView("org.argeo.security.ui.usersView");
		main.addView("org.argeo.security.ui.currentUserView");
	}

}
