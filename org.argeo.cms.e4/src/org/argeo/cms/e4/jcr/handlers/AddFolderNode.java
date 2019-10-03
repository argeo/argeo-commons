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
package org.argeo.cms.e4.jcr.handlers;

import java.util.List;

import javax.inject.Named;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeType;

import org.argeo.cms.e4.jcr.JcrBrowserView;
import org.argeo.cms.ui.jcr.model.SingleJcrNodeElem;
import org.argeo.cms.ui.jcr.model.WorkspaceElem;
import org.argeo.eclipse.ui.TreeParent;
import org.argeo.eclipse.ui.dialogs.ErrorFeedback;
import org.argeo.eclipse.ui.dialogs.SingleValue;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.services.IServiceConstants;
import org.eclipse.e4.ui.workbench.modeling.ESelectionService;

/**
 * Adds a node of type nt:folder, only on {@link SingleJcrNodeElem} and
 * {@link WorkspaceElem} TreeObject types.
 * 
 * This handler assumes that a selection provider is available and picks only
 * first selected item. It is UI's job to enable the command only when the
 * selection contains one and only one element. Thus no parameter is passed
 * through the command.
 */
public class AddFolderNode {
	@Execute
	public void execute(@Named(IServiceConstants.ACTIVE_PART) MPart part, ESelectionService selectionService) {
		List<?> selection = (List<?>) selectionService.getSelection();
		JcrBrowserView view = (JcrBrowserView) part.getObject();

		if (selection != null && selection.size() == 1) {
			TreeParent treeParentNode = null;
			Node jcrParentNode = null;
			Object obj = selection.get(0);

			if (obj instanceof SingleJcrNodeElem) {
				treeParentNode = (TreeParent) obj;
				jcrParentNode = ((SingleJcrNodeElem) treeParentNode).getNode();
			} else if (obj instanceof WorkspaceElem) {
				treeParentNode = (TreeParent) obj;
				jcrParentNode = ((WorkspaceElem) treeParentNode).getRootNode();
			} else
				return;

			String folderName = SingleValue.ask("Folder name", "Enter folder name");
			if (folderName != null) {
				try {
					jcrParentNode.addNode(folderName, NodeType.NT_FOLDER);
					jcrParentNode.getSession().save();
					view.nodeAdded(treeParentNode);
				} catch (RepositoryException e) {
					ErrorFeedback.show("Cannot create folder " + folderName + " under " + treeParentNode, e);
				}
			}
		} else {
			// ErrorFeedback.show(WorkbenchUiPlugin
			// .getMessage("errorUnvalidNtFolderNodeType"));
			ErrorFeedback.show("Invalid NT folder node type");
		}
	}

}
