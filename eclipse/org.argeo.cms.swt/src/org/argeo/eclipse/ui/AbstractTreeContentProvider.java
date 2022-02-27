package org.argeo.eclipse.ui;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

/**
 * Tree content provider dealing with tree objects and providing reasonable
 * defaults.
 */
public abstract class AbstractTreeContentProvider implements
		ITreeContentProvider {
	private static final long serialVersionUID = 8246126401957763868L;

	/** Does nothing */
	public void dispose() {
	}

	/** Does nothing */
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
	}

	public Object[] getChildren(Object element) {
		if (element instanceof TreeParent) {
			return ((TreeParent) element).getChildren();
		}
		return new Object[0];
	}

	public Object getParent(Object element) {
		if (element instanceof TreeParent) {
			return ((TreeParent) element).getParent();
		}
		return null;
	}

	public boolean hasChildren(Object element) {
		if (element instanceof TreeParent) {
			return ((TreeParent) element).hasChildren();
		}
		return false;
	}
}