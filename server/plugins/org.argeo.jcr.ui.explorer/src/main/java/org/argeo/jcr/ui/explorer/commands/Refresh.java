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
package org.argeo.jcr.ui.explorer.commands;

import java.util.Iterator;

import org.argeo.eclipse.ui.TreeParent;
import org.argeo.eclipse.ui.jcr.views.AbstractJcrBrowser;
import org.argeo.jcr.ui.explorer.JcrExplorerPlugin;
import org.argeo.jcr.ui.explorer.utils.JcrUiUtils;
import org.argeo.jcr.ui.explorer.views.GenericJcrBrowser;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;

/**
 * Force the selected objects of the active view to be refreshed doing the
 * following:
 * <ol>
 * <li>The model objects are recomputed</li>
 * <li>the view is refreshed</li>
 * </ol>
 */
public class Refresh extends AbstractHandler {

	public final static String ID = JcrExplorerPlugin.ID + ".refresh";

	public Object execute(ExecutionEvent event) throws ExecutionException {

		AbstractJcrBrowser view = (AbstractJcrBrowser) JcrExplorerPlugin
				.getDefault().getWorkbench().getActiveWorkbenchWindow()
				.getActivePage().getActivePart();//

		ISelection selection = JcrExplorerPlugin.getDefault().getWorkbench()
				.getActiveWorkbenchWindow().getActivePage().getSelection();

		if (selection != null && selection instanceof IStructuredSelection
				&& !selection.isEmpty()) {
			Iterator<?> it = ((IStructuredSelection) selection).iterator();
			while (it.hasNext()) {
				Object obj = it.next();
				if (obj instanceof TreeParent) {
					TreeParent tp = (TreeParent) obj;
					JcrUiUtils.forceRefreshIfNeeded(tp);
					view.refresh(obj);
				}
			}
		} else if (view instanceof GenericJcrBrowser)
			((GenericJcrBrowser) view).refresh(null); // force full refresh

		return null;
	}
}
