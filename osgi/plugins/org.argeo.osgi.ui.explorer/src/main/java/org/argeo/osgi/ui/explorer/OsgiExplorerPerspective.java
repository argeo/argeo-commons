package org.argeo.osgi.ui.explorer;

import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;

/** OSGi explorer perspective (to be enriched declaratively) */
public class OsgiExplorerPerspective implements IPerspectiveFactory {

	public void createInitialLayout(IPageLayout layout) {
		layout.setEditorAreaVisible(true);
		layout.setFixed(false);

	}

}
