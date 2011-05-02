package org.argeo.eclipse.ui.jcr;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.argeo.ArgeoException;
import org.argeo.eclipse.ui.AbstractTreeContentProvider;

/** Canonic implementation of tree content provider manipulating JCR nodes. */
public abstract class AbstractNodeContentProvider extends
		AbstractTreeContentProvider {
	private final static Log log = LogFactory
			.getLog(AbstractNodeContentProvider.class);

	private Session session;

	public AbstractNodeContentProvider(Session session) {
		this.session = session;
	}

	/**
	 * Whether this path is a base path (and thus has no parent). By default it
	 * returns true if path is '/' (root node)
	 */
	protected Boolean isBasePath(String path) {
		// root node
		return path.equals("/");
	}

	@Override
	public Object[] getChildren(Object element) {
		Object[] children;
		if (element instanceof Node) {
			try {
				Node node = (Node) element;
				children = getChildren(node);
			} catch (RepositoryException e) {
				throw new ArgeoException("Cannot get children of " + element, e);
			}
		} else if (element instanceof WrappedNode) {
			WrappedNode wrappedNode = (WrappedNode) element;
			try {
				children = getChildren(wrappedNode.getNode());
			} catch (RepositoryException e) {
				throw new ArgeoException("Cannot get children of "
						+ wrappedNode, e);
			}
		} else if (element instanceof NodesWrapper) {
			NodesWrapper node = (NodesWrapper) element;
			children = node.getChildren();
		} else {
			children = super.getChildren(element);
		}

		children = sort(element, children);
		return children;
	}

	/** Do not sort by default. To be overidden to provide custom sort. */
	protected Object[] sort(Object parent, Object[] children) {
		return children;
	}

	/**
	 * To be overridden in order to filter out some nodes. Does nothing by
	 * default. The provided list is a temporary one and can thus be modified
	 * directly . (e.g. via an iterator)
	 */
	protected List<Node> filterChildren(List<Node> children)
			throws RepositoryException {
		return children;
	}

	protected Object[] getChildren(Node node) throws RepositoryException {
		List<Node> nodes = new ArrayList<Node>();
		for (NodeIterator nit = node.getNodes(); nit.hasNext();)
			nodes.add(nit.nextNode());
		nodes = filterChildren(nodes);
		return nodes.toArray();
	}

	@Override
	public Object getParent(Object element) {
		if (element instanceof Node) {
			Node node = (Node) element;
			try {
				String path = node.getPath();
				if (isBasePath(path))
					return null;
				else
					return node.getParent();
			} catch (RepositoryException e) {
				log.warn("Cannot get parent of " + element + ": " + e);
				return null;
			}
		} else if (element instanceof WrappedNode) {
			WrappedNode wrappedNode = (WrappedNode) element;
			return wrappedNode.getParent();
		} else if (element instanceof NodesWrapper) {
			NodesWrapper nodesWrapper = (NodesWrapper) element;
			return this.getParent(nodesWrapper.getNode());
		}
		return super.getParent(element);
	}

	@Override
	public boolean hasChildren(Object element) {
		try {
			if (element instanceof Node) {
				Node node = (Node) element;
				return node.hasNodes();
			} else if (element instanceof WrappedNode) {
				WrappedNode wrappedNode = (WrappedNode) element;
				return wrappedNode.getNode().hasNodes();
			} else if (element instanceof NodesWrapper) {
				NodesWrapper nodesWrapper = (NodesWrapper) element;
				return nodesWrapper.hasChildren();
			}

		} catch (RepositoryException e) {
			throw new ArgeoException("Cannot check whether " + element
					+ " has children", e);
		}
		return super.hasChildren(element);
	}

	public Session getSession() {
		return session;
	}
}
