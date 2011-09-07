package org.argeo.jcr.ui.explorer;

import org.eclipse.ui.IFolderLayout;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;

/** Base perspective for JcrExplorer browser */
public class JcrExplorerPerspective implements IPerspectiveFactory {
	public static String BROWSER_VIEW_ID = JcrExplorerPlugin.ID
			+ ".browserView";

	public void createInitialLayout(IPageLayout layout) {
		layout.setEditorAreaVisible(true);

		IFolderLayout upperLeft = layout.createFolder(JcrExplorerPlugin.ID
				+ ".upperLeft", IPageLayout.LEFT, 0.4f, layout.getEditorArea());
		upperLeft.addView(BROWSER_VIEW_ID);
	}

}
