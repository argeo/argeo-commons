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
		getNodeViewer().refresh(obj);
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
