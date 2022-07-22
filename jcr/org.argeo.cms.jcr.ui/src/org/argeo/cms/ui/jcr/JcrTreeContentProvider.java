package org.argeo.cms.ui.jcr;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.argeo.eclipse.ui.EclipseUiException;
import org.argeo.eclipse.ui.jcr.util.JcrItemsComparator;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

/**
 * Implementation of the {@code ITreeContentProvider} in order to display a
 * single JCR node and its children in a tree like structure
 */
public class JcrTreeContentProvider implements ITreeContentProvider {
	private static final long serialVersionUID = -2128326504754297297L;
	// private Node rootNode;
	private JcrItemsComparator itemComparator = new JcrItemsComparator();

	/**
	 * Sends back the first level of the Tree. input element must be a single node
	 * object
	 */
	public Object[] getElements(Object inputElement) {
		Node rootNode = (Node) inputElement;
		return childrenNodes(rootNode);
	}

	public Object[] getChildren(Object parentElement) {
		return childrenNodes((Node) parentElement);
	}

	public Object getParent(Object element) {
		try {
			Node node = (Node) element;
			if (!node.getPath().equals("/"))
				return node.getParent();
			else
				return null;
		} catch (RepositoryException e) {
			return null;
		}
	}

	public boolean hasChildren(Object element) {
		try {
			return ((Node) element).hasNodes();
		} catch (RepositoryException e) {
			throw new EclipseUiException("Cannot check children existence on " + element, e);
		}
	}

	protected Object[] childrenNodes(Node parentNode) {
		try {
			List<Node> children = new ArrayList<Node>();
			NodeIterator nit = parentNode.getNodes();
			while (nit.hasNext()) {
				Node node = nit.nextNode();
//				if (node.getName().startsWith("rep:") || node.getName().startsWith("jcr:")
//						|| node.getName().startsWith("nt:"))
//					continue nodes;
				children.add(node);
			}
			Node[] arr = children.toArray(new Node[0]);
			Arrays.sort(arr, itemComparator);
			return arr;
		} catch (RepositoryException e) {
			throw new EclipseUiException("Cannot list children of " + parentNode, e);
		}
	}

	public void dispose() {
	}

	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
	}
}
