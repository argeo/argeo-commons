package org.argeo.demo.i18n;

import org.argeo.demo.i18n.views.SimpleTreeView;
import org.eclipse.ui.IFolderLayout;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;

/** Base perspective for JcrExplorer browser */
public class I18nDemoPerspective implements IPerspectiveFactory {

	public void createInitialLayout(IPageLayout layout) {
		layout.setEditorAreaVisible(true);

		IFolderLayout upperLeft = layout.createFolder(I18nDemoPlugin.ID
				+ ".upperLeft", IPageLayout.LEFT, 0.4f, layout.getEditorArea());
		upperLeft.addView(SimpleTreeView.ID);
	}
}
