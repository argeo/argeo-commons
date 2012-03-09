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
package org.argeo.eclipse.ui.jcr.views;

import javax.jcr.Node;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.part.ViewPart;

public abstract class AbstractJcrBrowser extends ViewPart {

	@Override
	public abstract void createPartControl(Composite parent);

	/**
	 * To be overridden to adapt size of form and result frames.
	 */
	abstract protected int[] getWeights();

	/**
	 * To be overridden to provide an adapted size nodeViewer
	 */
	abstract protected TreeViewer createNodeViewer(Composite parent,
			ITreeContentProvider nodeContentProvider);

	/**
	 * To be overridden to retrieve the current nodeViewer
	 */
	abstract protected TreeViewer getNodeViewer();

	/*
	 * Enables the refresh of the tree.
	 */
	@Override
	public void setFocus() {
		getNodeViewer().getTree().setFocus();
	}

	public void refresh(Object obj) {
		// getNodeViewer().update(obj, null);
		getNodeViewer().refresh(obj);
		// getNodeViewer().expandToLevel(obj, 1);
	}

	public void nodeAdded(Node parentNode, Node newNode) {
		getNodeViewer().refresh(parentNode);
		getNodeViewer().expandToLevel(newNode, 0);
	}

	public void nodeRemoved(Node parentNode) {
		IStructuredSelection newSel = new StructuredSelection(parentNode);
		getNodeViewer().setSelection(newSel, true);
		// Force refresh
		IStructuredSelection tmpSel = (IStructuredSelection) getNodeViewer()
				.getSelection();
		getNodeViewer().refresh(tmpSel.getFirstElement());
	}
}
