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

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeType;

import org.argeo.eclipse.ui.ErrorFeedback;
import org.argeo.eclipse.ui.TreeParent;
import org.argeo.eclipse.ui.dialogs.SingleValue;
import org.argeo.eclipse.ui.jcr.JcrUiPlugin;
import org.argeo.jcr.ui.explorer.JcrExplorerPlugin;
import org.argeo.jcr.ui.explorer.model.SingleJcrNode;
import org.argeo.jcr.ui.explorer.model.WorkspaceNode;
import org.argeo.jcr.ui.explorer.views.GenericJcrBrowser;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.handlers.HandlerUtil;

/**
 * Adds a node of type nt:folder, only on {@link SingleJcrNode} and
 * {@link WorkspaceNode} TreeObject types.
 * 
 * 
 * This handler assumes that a selection provider is available and picks only
 * first selected item. It is UI's job to enable the command only when the
 * selection contains one and only one element. Thus no parameter is passed
 * through the command.
 * 
 * This handler is still 'hard linked' to a GenericJcrBrowser view to enable
 * correct tree refresh when a node is added. This must be corrected in future
 * versions.
 */
public class AddFolderNode extends AbstractHandler {

	public final static String ID = JcrExplorerPlugin.ID + ".addFolderNode";

	// public final static String DEFAULT_LABEL = JcrExplorerPlugin
	// .getMessage("addFolderNodeCmdLbl");
	// public final static String DEFAULT_ICON_REL_PATH = "icons/addRepo.gif";

	public Object execute(ExecutionEvent event) throws ExecutionException {

		ISelection selection = HandlerUtil.getActiveWorkbenchWindow(event)
				.getActivePage().getSelection();

		GenericJcrBrowser view = (GenericJcrBrowser) HandlerUtil
				.getActiveWorkbenchWindow(event).getActivePage()
				.findView(HandlerUtil.getActivePartId(event));

		if (selection != null && !selection.isEmpty()
				&& selection instanceof IStructuredSelection) {
			Object obj = ((IStructuredSelection) selection).getFirstElement();
			TreeParent treeParentNode = null;
			Node jcrParentNode = null;

			if (obj instanceof SingleJcrNode) {
				treeParentNode = (TreeParent) obj;
				jcrParentNode = ((SingleJcrNode) treeParentNode).getNode();
			} else if (obj instanceof WorkspaceNode) {
				treeParentNode = (TreeParent) obj;
				jcrParentNode = ((WorkspaceNode) treeParentNode).getRootNode();
			} else
				return null;

			String folderName = SingleValue.ask("Folder name",
					"Enter folder name");
			if (folderName != null) {
				try {
					jcrParentNode.addNode(folderName, NodeType.NT_FOLDER);
					jcrParentNode.getSession().save();
					view.nodeAdded(treeParentNode);
				} catch (RepositoryException e) {
					ErrorFeedback.show("Cannot create folder " + folderName
							+ " under " + treeParentNode, e);
				}
			}
		} else {
			ErrorFeedback.show(JcrUiPlugin
					.getMessage("errorUnvalidNtFolderNodeType"));
		}
		return null;
	}
}
