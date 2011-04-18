package org.argeo.eclipse.ui.jcr;

import javax.jcr.Node;

/** Wraps a node (created from a {@link NodesWrapper}) */
public class WrappedNode {
	private final NodesWrapper parent;
	private final Node node;

	public WrappedNode(NodesWrapper parent, Node node) {
		super();
		this.parent = parent;
		this.node = node;
	}

	public NodesWrapper getParent() {
		return parent;
	}

	public Node getNode() {
		return node;
	}

	public String toString() {
		return "wrapped " + node;
	}

	@Override
	public int hashCode() {
		return node.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof WrappedNode)
			return node.equals(((WrappedNode) obj).getNode());
		else
			return false;
	}

}
