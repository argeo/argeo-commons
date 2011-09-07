package org.argeo.jcr.ui.explorer.providers;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeType;
import javax.jcr.version.Version;
import javax.jcr.version.VersionHistory;
import javax.jcr.version.VersionIterator;
import javax.jcr.version.VersionManager;

import org.argeo.ArgeoException;
import org.argeo.jcr.ui.explorer.browser.ItemComparator;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

/**
 * Implementation of the {@code ITreeContentProvider} in order to display some
 * version informations of a JCR full versionable node in a tree like structure
 * 
 */
public class FullVersioningTreeContentProvider implements ITreeContentProvider {
	// private Node rootNode;
	private ItemComparator itemComparator = new ItemComparator();

	/**
	 * Sends back the first level of the Tree. input element must be a single
	 * node object
	 */
	public Object[] getElements(Object inputElement) {
		try {
			Node rootNode = (Node) inputElement;
			String curPath = rootNode.getPath();
			VersionManager vm = rootNode.getSession().getWorkspace()
					.getVersionManager();

			VersionHistory vh = vm.getVersionHistory(curPath);
			List<Version> result = new ArrayList<Version>();
			VersionIterator vi = vh.getAllLinearVersions();

			while (vi.hasNext()) {
				result.add(vi.nextVersion());
			}
			return result.toArray();
		} catch (RepositoryException re) {
			throw new ArgeoException(
					"Unexpected error while getting version elements", re);
		}
	}

	public Object[] getChildren(Object parentElement) {
		try {
			if (parentElement instanceof Version) {
				List<Node> tmp = new ArrayList<Node>();
				tmp.add(((Version) parentElement).getFrozenNode());
				return tmp.toArray();
				// Node node = ((Version) parentElement).getFrozenNode();
				// return new Object[] { node };
			}
		} catch (RepositoryException re) {
			throw new ArgeoException("Unexpected error while getting child "
					+ "node for version element", re);
		}
		return null;
	}

	public Object getParent(Object element) {
		try {
			// this will not work in a simpleVersionning environment, parent is
			// not a node.
			if (element instanceof Node
					&& ((Node) element).isNodeType(NodeType.NT_FROZEN_NODE)) {
				Node node = (Node) element;
				return node.getParent();
			} else
				return null;
		} catch (RepositoryException e) {
			return null;
		}
	}

	public boolean hasChildren(Object element) {
		try {
			if (element instanceof Version)
				return true;
			else if (element instanceof Node)
				return ((Node) element).hasNodes();
			else
				return false;
		} catch (RepositoryException e) {
			throw new ArgeoException("Cannot check children of " + element, e);
		}
	}

	public void dispose() {
	}

	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
	}

}
