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
package org.argeo.eclipse.ui.workbench.jcr;

import javax.jcr.Node;

import org.argeo.ArgeoException;
import org.argeo.eclipse.ui.workbench.WorkbenchUiPlugin;
import org.argeo.eclipse.ui.workbench.jcr.internal.parts.ChildNodesPage;
import org.argeo.eclipse.ui.workbench.jcr.internal.parts.GenericNodeEditorInput;
import org.argeo.eclipse.ui.workbench.jcr.internal.parts.GenericPropertyPage;
import org.argeo.eclipse.ui.workbench.jcr.internal.parts.NodeRightsManagementPage;
import org.argeo.eclipse.ui.workbench.jcr.internal.parts.NodeVersionHistoryPage;
import org.argeo.jcr.JcrUtils;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.editor.FormEditor;

/**
 * Container for the node editor page. At creation time, it takes a JCR Node
 * that cannot be changed afterwards.
 */
public class DefaultNodeEditor extends FormEditor {
	private static final long serialVersionUID = -5397680152514917137L;

	// private final static Log log =
	// LogFactory.getLog(GenericNodeEditor.class);
	public final static String ID = WorkbenchUiPlugin.ID + ".defaultNodeEditor";

	private Node currentNode;

	private GenericPropertyPage genericPropertyPage;
	private ChildNodesPage childNodesPage;
	private NodeRightsManagementPage nodeRightsManagementPage;
	private NodeVersionHistoryPage nodeVersionHistoryPage;

	public void init(IEditorSite site, IEditorInput input)
			throws PartInitException {
		super.init(site, input);
		GenericNodeEditorInput nei = (GenericNodeEditorInput) getEditorInput();
		currentNode = nei.getCurrentNode();
		this.setPartName(JcrUtils.lastPathElement(nei.getPath()));
	}

	@Override
	protected void addPages() {
		try {
			// genericNodePage = new GenericNodePage(this,
			// JcrExplorerPlugin.getMessage("genericNodePageTitle"),
			// currentNode);
			// addPage(genericNodePage);

			genericPropertyPage = new GenericPropertyPage(this,
					WorkbenchUiPlugin.getMessage("genericNodePageTitle"),
					currentNode);
			addPage(genericPropertyPage);

			childNodesPage = new ChildNodesPage(this,
					WorkbenchUiPlugin.getMessage("childNodesPageTitle"),
					currentNode);
			addPage(childNodesPage);

			nodeRightsManagementPage = new NodeRightsManagementPage(this,
					WorkbenchUiPlugin
							.getMessage("nodeRightsManagementPageTitle"),
					currentNode);
			addPage(nodeRightsManagementPage);

			nodeVersionHistoryPage = new NodeVersionHistoryPage(
					this,
					WorkbenchUiPlugin.getMessage("nodeVersionHistoryPageTitle"),
					currentNode);
			addPage(nodeVersionHistoryPage);
		} catch (PartInitException e) {
			throw new ArgeoException("Not able to add an empty page ", e);
		}
	}

	@Override
	public void doSaveAs() {
		// unused compulsory method
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
		try {
			// Automatically commit all pages of the editor
			commitPages(true);
			firePropertyChange(PROP_DIRTY);
		} catch (Exception e) {
			throw new ArgeoException("Error while saving node", e);
		}

	}

	@Override
	public boolean isSaveAsAllowed() {
		return true;
	}

	Node getCurrentNode() {
		return currentNode;
	}
}
