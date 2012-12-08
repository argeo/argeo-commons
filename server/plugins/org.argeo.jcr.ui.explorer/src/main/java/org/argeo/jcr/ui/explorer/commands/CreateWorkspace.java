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

import java.util.Arrays;

import org.argeo.eclipse.ui.ErrorFeedback;
import org.argeo.eclipse.ui.dialogs.SingleValue;
import org.argeo.jcr.ui.explorer.JcrExplorerPlugin;
import org.argeo.jcr.ui.explorer.model.RepositoryNode;
import org.argeo.jcr.ui.explorer.views.GenericJcrBrowser;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.handlers.HandlerUtil;

/** Creates a new JCR workspace */
public class CreateWorkspace extends AbstractHandler {

	public final static String ID = JcrExplorerPlugin.ID + ".addFolderNode";

	public Object execute(ExecutionEvent event) throws ExecutionException {

		ISelection selection = HandlerUtil.getActiveWorkbenchWindow(event)
				.getActivePage().getSelection();

		GenericJcrBrowser view = (GenericJcrBrowser) HandlerUtil
				.getActiveWorkbenchWindow(event).getActivePage()
				.findView(HandlerUtil.getActivePartId(event));

		if (selection != null && !selection.isEmpty()
				&& selection instanceof IStructuredSelection) {
			Object obj = ((IStructuredSelection) selection).getFirstElement();
			if (!(obj instanceof RepositoryNode))
				return null;

			RepositoryNode repositoryNode = (RepositoryNode) obj;
			String workspaceName = SingleValue.ask("Workspace name",
					"Enter workspace name");
			if (workspaceName != null) {
				if (Arrays.asList(repositoryNode.getAccessibleWorkspaceNames())
						.contains(workspaceName)) {
					ErrorFeedback.show("Workspace " + workspaceName
							+ " already exists.");
				} else {
					repositoryNode.createWorkspace(workspaceName);
					view.nodeAdded(repositoryNode);
				}
			}
		} else {
			ErrorFeedback.show("Cannot create workspace");
		}
		return null;
	}
}
