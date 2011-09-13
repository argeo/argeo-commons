package org.argeo.eclipse.ui.jcr;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.argeo.ArgeoException;

/**
 * Element of tree which is based on a node, but whose children are not
 * necessarily this node children.
 */
public class NodesWrapper {
	private final Node node;

	public NodesWrapper(Node node) {
		super();
		this.node = node;
	}

	protected NodeIterator getNodeIterator() throws RepositoryException {
		return node.getNodes();
	}

	protected List<WrappedNode> getWrappedNodes() throws RepositoryException {
		List<WrappedNode> nodes = new ArrayList<WrappedNode>();
		for (NodeIterator nit = getNodeIterator(); nit.hasNext();)
			nodes.add(new WrappedNode(this, nit.nextNode()));
		return nodes;
	}

	public Object[] getChildren() {
		try {
			return getWrappedNodes().toArray();
		} catch (RepositoryException e) {
			throw new ArgeoException("Cannot get wrapped children", e);
		}
	}

	/**
	 * @return true by default because we don't want to compute the wrapped
	 *         nodes twice
	 */
	public Boolean hasChildren() {
		return true;
	}

	public Node getNode() {
		return node;
	}

	@Override
	public int hashCode() {
		return node.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof NodesWrapper)
			return node.equals(((NodesWrapper) obj).getNode());
		else
			return false;
	}

	public String toString() {
		return "nodes wrapper based on " + node;
	}
}
