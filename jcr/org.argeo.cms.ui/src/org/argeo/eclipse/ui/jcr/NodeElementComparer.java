package org.argeo.eclipse.ui.jcr;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.argeo.eclipse.ui.EclipseUiException;
import org.eclipse.jface.viewers.IElementComparer;

/** Element comparer for JCR node, to be used in JFace viewers. */
public class NodeElementComparer implements IElementComparer {

	public boolean equals(Object a, Object b) {
		try {
			if ((a instanceof Node) && (b instanceof Node)) {
				Node nodeA = (Node) a;
				Node nodeB = (Node) b;
				return nodeA.getIdentifier().equals(nodeB.getIdentifier());
			} else {
				return a.equals(b);
			}
		} catch (RepositoryException e) {
			throw new EclipseUiException("Cannot compare nodes", e);
		}
	}

	public int hashCode(Object element) {
		try {
			if (element instanceof Node)
				return ((Node) element).getIdentifier().hashCode();
			return element.hashCode();
		} catch (RepositoryException e) {
			throw new EclipseUiException("Cannot get hash code", e);
		}
	}

}
