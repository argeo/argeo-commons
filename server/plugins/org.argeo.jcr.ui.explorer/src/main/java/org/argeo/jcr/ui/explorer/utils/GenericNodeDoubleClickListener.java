/*
 * Copyright (C) 2007-2012 Mathieu Baudier
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
package org.argeo.jcr.ui.explorer.utils;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeType;

import org.argeo.ArgeoException;
import org.argeo.eclipse.ui.jcr.utils.JcrFileProvider;
import org.argeo.eclipse.ui.specific.FileHandler;
import org.argeo.jcr.ui.explorer.JcrExplorerPlugin;
import org.argeo.jcr.ui.explorer.editors.GenericNodeEditor;
import org.argeo.jcr.ui.explorer.editors.GenericNodeEditorInput;
import org.argeo.jcr.ui.explorer.model.RepositoryNode;
import org.argeo.jcr.ui.explorer.model.SingleJcrNode;
import org.argeo.jcr.ui.explorer.model.WorkspaceNode;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.ui.PartInitException;

/**
 * Centralizes the management of double click on a NodeTreeViewer
 */
public class GenericNodeDoubleClickListener implements IDoubleClickListener {

	// private final static Log log = LogFactory
	// .getLog(GenericNodeDoubleClickListener.class);

	private TreeViewer nodeViewer;
	private JcrFileProvider jfp;
	private FileHandler fileHandler;

	public GenericNodeDoubleClickListener(TreeViewer nodeViewer) {
		this.nodeViewer = nodeViewer;
		jfp = new JcrFileProvider();
		fileHandler = new FileHandler(jfp);
	}

	public void doubleClick(DoubleClickEvent event) {
		if (event.getSelection() == null || event.getSelection().isEmpty())
			return;
		Object obj = ((IStructuredSelection) event.getSelection())
				.getFirstElement();
		if (obj instanceof RepositoryNode) {
			RepositoryNode rpNode = (RepositoryNode) obj;
			if (rpNode.getChildren().length == 0) {
				rpNode.login();
				nodeViewer.refresh(obj);
			}
			// else do nothing
		} else if (obj instanceof WorkspaceNode) {
			((WorkspaceNode) obj).login();
			nodeViewer.refresh(obj);
		} else if (obj instanceof SingleJcrNode) {
			SingleJcrNode sjn = (SingleJcrNode) obj;
			Node node = sjn.getNode();
			try {
				if (node.isNodeType(NodeType.NT_FILE)) {
					// double click on a file node triggers its opening
					String name = node.getName();
					String id = node.getIdentifier();

					// For the file provider to be able to browse the
					// various
					// repository.
					// TODO : enhanced that.
					// ITreeContentProvider itcp = (ITreeContentProvider)
					// nodeViewer
					// .getContentProvider();
					jfp.setReferenceNode(node);
					fileHandler.openFile(name, id);
				}
				GenericNodeEditorInput gnei = new GenericNodeEditorInput(node);
				JcrExplorerPlugin.getDefault().getWorkbench()
						.getActiveWorkbenchWindow().getActivePage()
						.openEditor(gnei, GenericNodeEditor.ID);
			} catch (RepositoryException re) {
				throw new ArgeoException(
						"Repository error while getting node info", re);
			} catch (PartInitException pie) {
				throw new ArgeoException(
						"Unexepected exception while opening node editor", pie);
			}
		}
	}
}
