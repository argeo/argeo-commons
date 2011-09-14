package org.argeo.jcr.ui.explorer.model;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Workspace;

import org.argeo.ArgeoException;
import org.argeo.eclipse.ui.TreeParent;

/**
 * UI Tree component. Wraps a node of a JCR {@link Workspace}. It also keeps a
 * reference to its parent node that can either be a {@link WorkspaceNode}, a
 * {@link SingleJcrNode} or null if the node is "mounted" as the root of the UI
 * tree.
 */

public class SingleJcrNode extends TreeParent implements UiNode {

	private final Node node;
	private String alias = null;

	// keeps a local reference to the node's name to avoid exception when the
	// session is lost
	// private final String name;

	/** Creates a new UiNode in the UI Tree */
	public SingleJcrNode(TreeParent parent, Node node, String name) {
		super(name);
		setParent(parent);
		this.node = node;
	}

	/**
	 * Creates a new UiNode in the UI Tree, keeping a reference to the alias of
	 * the corresponding repository in the current UI environment. It is useful
	 * to be able to mount nodes as roots of the UI tree.
	 */
	public SingleJcrNode(TreeParent parent, Node node, String name, String alias) {
		super(name);
		setParent(parent);
		this.node = node;
		this.alias = alias;
	}

	/** returns the node wrapped by the current Ui object */
	public Node getNode() {
		return node;
	}

	/**
	 * Returns the alias corresponding to the repository abstraction that
	 * contains current node. If the current object is mounted as root of the UI
	 * tree, the alias is stored in the object. Otherwise, we must browse the
	 * tree backward to the RepositoryNode.
	 * 
	 * Alias is then cached in the current object so that next time it will be
	 * here.
	 */
	public String getAlias() {
		if (alias == null) {
			alias = ((UiNode) getParent()).getAlias();
		}
		return alias;
	}

	/**
	 * Override normal behaviour to initialize children only when first
	 * requested
	 */
	@Override
	public synchronized Object[] getChildren() {
		if (isLoaded()) {
			return super.getChildren();
		} else {
			// initialize current object
			try {
				NodeIterator ni = node.getNodes();
				while (ni.hasNext()) {
					Node curNode = ni.nextNode();
					addChild(new SingleJcrNode(this, curNode, curNode.getName()));
				}
				return super.getChildren();
			} catch (RepositoryException re) {
				throw new ArgeoException(
						"Unexcpected error while initializing children SingleJcrNode",
						re);
			}
		}
	}

	@Override
	public boolean hasChildren() {
		try {
			return node.hasNodes();
		} catch (RepositoryException re) {
			throw new ArgeoException(
					"Unexpected error while checking children node existence",
					re);
		}
	}

}
