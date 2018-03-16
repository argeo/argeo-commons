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
package org.argeo.cms.ui.jcr;

import javax.jcr.Node;

import org.argeo.cms.ui.jcr.model.RepositoryElem;
import org.argeo.cms.ui.jcr.model.SingleJcrNodeElem;
import org.argeo.cms.ui.jcr.model.WorkspaceElem;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;

/** Centralizes the management of double click on a NodeTreeViewer */
public class JcrDClickListener implements IDoubleClickListener {
	// private final static Log log = LogFactory
	// .getLog(GenericNodeDoubleClickListener.class);

	private TreeViewer nodeViewer;

	// private JcrFileProvider jfp;
	// private FileHandler fileHandler;

	public JcrDClickListener(TreeViewer nodeViewer) {
		this.nodeViewer = nodeViewer;
		// jfp = new JcrFileProvider();
		// Commented out. see https://www.argeo.org/bugzilla/show_bug.cgi?id=188
		// fileHandler = null;
		// fileHandler = new FileHandler(jfp);
	}

	public void doubleClick(DoubleClickEvent event) {
		if (event.getSelection() == null || event.getSelection().isEmpty())
			return;
		Object obj = ((IStructuredSelection) event.getSelection()).getFirstElement();
		if (obj instanceof RepositoryElem) {
			RepositoryElem rpNode = (RepositoryElem) obj;
			if (rpNode.isConnected()) {
				rpNode.logout();
			} else {
				rpNode.login();
			}
			nodeViewer.refresh(obj);
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
			openNode(node);
		}
	}

	protected void openNode(Node node) {
		// TODO implement generic behaviour
	}
}
