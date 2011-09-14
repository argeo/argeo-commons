package org.argeo.sandbox.ui.rap;

import org.argeo.sandbox.ui.rap.views.TestDownloadView;
import org.eclipse.ui.IFolderLayout;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;

/**
 * Configures the perspective layout. This class is contributed through the
 * plugin.xml.
 */
public class Perspective implements IPerspectiveFactory {

	public final static String ID = "org.argeo.sandbox.ui.downloadTries";

	public void createInitialLayout(IPageLayout layout) {
		String editorArea = layout.getEditorArea();
		layout.setEditorAreaVisible(true);
		layout.setFixed(false);

		// Create the main ui layout
		IFolderLayout left = layout.createFolder("left", IPageLayout.LEFT,
				0.4f, editorArea);

		// add the views to the corresponding place holder
		left.addView(TestDownloadView.ID);
	}
}
