package org.argeo.eclipse.ui.jcr.explorer;

import org.eclipse.ui.IFolderLayout;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;

public class JcrExplorerPerspective implements IPerspectiveFactory {

	public void createInitialLayout(IPageLayout layout) {
		layout.setEditorAreaVisible(true);

		IFolderLayout main = layout.createFolder("upperLeft", IPageLayout.LEFT,
				0.5f, layout.getEditorArea());

	}

}
