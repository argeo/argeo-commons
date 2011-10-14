package org.argeo.jcr.ui.explorer.providers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.argeo.ArgeoException;
import org.argeo.eclipse.ui.jcr.utils.JcrItemsComparator;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

/**
 * Implementation of the {@code ITreeContentProvider} in order to display a
 * single JCR node and its children in a tree like structure
 * 
 */
public class SingleNodeAsTreeContentProvider implements ITreeContentProvider {
	// private Node rootNode;
	private JcrItemsComparator itemComparator = new JcrItemsComparator();

	/**
	 * Sends back the first level of the Tree. input element must be a single
	 * node object
	 */
	public Object[] getElements(Object inputElement) {
		try {
			Node rootNode = (Node) inputElement;
			List<Node> result = new ArrayList<Node>();
			NodeIterator ni = rootNode.getNodes();
			while (ni.hasNext()) {
				result.add(ni.nextNode());
			}

			return result.toArray();
		} catch (RepositoryException re) {
			throw new ArgeoException(
					"Unexpected error while getting child nodes for children editor page ",
					re);
		}
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
			throw new ArgeoException("Cannot check children of " + element, e);
		}
	}

	public void dispose() {
	}

	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
	}

	protected Object[] childrenNodes(Node parentNode) {
		try {
			List<Node> children = new ArrayList<Node>();
			NodeIterator nit = parentNode.getNodes();
			while (nit.hasNext()) {
				Node node = nit.nextNode();
				children.add(node);
			}
			Node[] arr = children.toArray(new Node[children.size()]);
			Arrays.sort(arr, itemComparator);
			return arr;
		} catch (RepositoryException e) {
			throw new ArgeoException("Cannot list children of " + parentNode, e);
		}
	}
}
