package org.argeo.eclipse.ui.jcr.utils;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.argeo.ArgeoException;
import org.eclipse.jface.viewers.IElementComparer;

public class NodeViewerComparer implements IElementComparer {

	// force comparison on Node IDs only.
	public boolean equals(Object elementA, Object elementB) {
		if (!(elementA instanceof Node) || !(elementB instanceof Node)) {
			return elementA == null ? elementB == null : elementA
					.equals(elementB);
		} else {

			boolean result = false;
			try {
				String idA = ((Node) elementA).getIdentifier();
				String idB = ((Node) elementB).getIdentifier();
				result = idA == null ? idB == null : idA.equals(idB);
			} catch (RepositoryException re) {
				throw new ArgeoException("cannot compare nodes", re);
			}

			return result;
		}
	}

	public int hashCode(Object element) {
		// TODO enhanced this method.
		return element.getClass().toString().hashCode();
	}
}