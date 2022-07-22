package org.argeo.cms.ui.jcr.model;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Workspace;

import org.argeo.cms.ux.widgets.TreeParent;
import org.argeo.eclipse.ui.EclipseUiException;

/**
 * UI Tree component. Wraps a node of a JCR {@link Workspace}. It also keeps a
 * reference to its parent node that can either be a {@link WorkspaceElem}, a
 * {@link SingleJcrNodeElem} or null if the node is "mounted" as the root of the
 * UI tree.
 */
public class SingleJcrNodeElem extends TreeParent {

	private final Node node;
	private String alias = null;

	/** Creates a new UiNode in the UI Tree */
	public SingleJcrNodeElem(TreeParent parent, Node node, String name) {
		super(name);
		setParent(parent);
		this.node = node;
	}

	/**
	 * Creates a new UiNode in the UI Tree, keeping a reference to the alias of
	 * the corresponding repository in the current UI environment. It is useful
	 * to be able to mount nodes as roots of the UI tree.
	 */
	public SingleJcrNodeElem(TreeParent parent, Node node, String name, String alias) {
		super(name);
		setParent(parent);
		this.node = node;
		this.alias = alias;
	}

	/** Returns the node wrapped by the current UI object */
	public Node getNode() {
		return node;
	}

	protected String getRepositoryAlias() {
		return alias;
	}

	/**
	 * Overrides normal behaviour to initialise children only when first
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
					addChild(new SingleJcrNodeElem(this, curNode, curNode.getName()));
				}
				return super.getChildren();
			} catch (RepositoryException re) {
				throw new EclipseUiException("Cannot initialize SingleJcrNode children", re);
			}
		}
	}

	@Override
	public boolean hasChildren() {
		try {
			if (node.getSession().isLive())
				return node.hasNodes();
			else
				return false;
		} catch (RepositoryException re) {
			throw new EclipseUiException("Cannot check children node existence", re);
		}
	}
}
