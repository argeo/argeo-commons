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
package org.argeo.eclipse.ui.workbench.jcr.internal;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeType;

import org.argeo.ArgeoException;
import org.argeo.eclipse.ui.workbench.WorkbenchUiPlugin;
import org.argeo.eclipse.ui.workbench.jcr.DefaultNodeEditor;
import org.argeo.eclipse.ui.workbench.jcr.internal.model.RepositoryElem;
import org.argeo.eclipse.ui.workbench.jcr.internal.model.SingleJcrNodeElem;
import org.argeo.eclipse.ui.workbench.jcr.internal.model.WorkspaceElem;
import org.argeo.eclipse.ui.workbench.jcr.internal.parts.GenericNodeEditorInput;
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

	// private JcrFileProvider jfp;
	// private FileHandler fileHandler;

	public GenericNodeDoubleClickListener(TreeViewer nodeViewer) {
		this.nodeViewer = nodeViewer;
		// jfp = new JcrFileProvider();
		// Commented out. see https://www.argeo.org/bugzilla/show_bug.cgi?id=188
		// fileHandler = null;
		// fileHandler = new FileHandler(jfp);
	}

	public void doubleClick(DoubleClickEvent event) {
		if (event.getSelection() == null || event.getSelection().isEmpty())
			return;
		Object obj = ((IStructuredSelection) event.getSelection())
				.getFirstElement();
		if (obj instanceof RepositoryElem) {
			RepositoryElem rpNode = (RepositoryElem) obj;
			if (!rpNode.isConnected()) {
				rpNode.login();
				nodeViewer.refresh(obj);
			}
		} else if (obj instanceof WorkspaceElem) {
			WorkspaceElem wn = (WorkspaceElem) obj;
			if (wn.isConnected())
				wn.logout();
			else
				wn.login();
			nodeViewer.refresh(obj);
		} else if (obj instanceof SingleJcrNodeElem) {
			SingleJcrNodeElem sjn = (SingleJcrNodeElem) obj;
			Node node = sjn.getNode();
			try {
				if (node.isNodeType(NodeType.NT_FILE)) {
					// double click on a file node triggers its opening
					String name = node.getName();
					String id = node.getIdentifier();

					// TODO add integration of direct retrieval of the binary in
					// a JCR repo.
					// Map<String, String> params = new HashMap<String,
					// String>();
					// params.put(OpenFile.PARAM_FILE_NAME, name);
					// params.put(OpenFile.PARAM_FILE_URI, "jcr://" + id);
					// CommandUtils
					// .callCommand("org.argeo.security.ui.specific.openFile",
					// params);

					// For the file provider to be able to browse the
					// various
					// repository.
					// TODO : enhanced that.
					// ITreeContentProvider itcp = (ITreeContentProvider)
					// nodeViewer
					// .getContentProvider();
					// jfp.setReferenceNode(node);
					// if (fileHandler != null)
					// fileHandler.openFile(name, id);
				}
				GenericNodeEditorInput gnei = new GenericNodeEditorInput(node);
				WorkbenchUiPlugin.getDefault().getWorkbench()
						.getActiveWorkbenchWindow().getActivePage()
						.openEditor(gnei, DefaultNodeEditor.ID);
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