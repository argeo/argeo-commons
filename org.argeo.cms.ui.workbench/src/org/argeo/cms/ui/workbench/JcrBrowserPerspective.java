/*
 * Copyright (C) 2007-2012 Argeo GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.argeo.cms.ui.workbench;

import org.argeo.cms.ui.workbench.jcr.JcrBrowserView;
import org.argeo.cms.ui.workbench.jcr.NodeFsBrowserView;
import org.eclipse.ui.IFolderLayout;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;

/** Base perspective for the Jcr browser */
public class JcrBrowserPerspective implements IPerspectiveFactory {

	public void createInitialLayout(IPageLayout layout) {
		layout.setEditorAreaVisible(true);
		IFolderLayout upperLeft = layout.createFolder(
				WorkbenchUiPlugin.PLUGIN_ID + ".upperLeft", IPageLayout.LEFT,
				0.4f, layout.getEditorArea());
		upperLeft.addView(JcrBrowserView.ID);
		upperLeft.addView(NodeFsBrowserView.ID);
	}
}
