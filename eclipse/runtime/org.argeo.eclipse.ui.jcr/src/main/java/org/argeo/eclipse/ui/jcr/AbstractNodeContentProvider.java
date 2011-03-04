package org.argeo.eclipse.ui.jcr;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.argeo.ArgeoException;
import org.argeo.eclipse.ui.AbstractTreeContentProvider;

/** Canonic implementation of tree content provider manipulating JCR nodes. */
public abstract class AbstractNodeContentProvider extends
		AbstractTreeContentProvider {
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
		if (element instanceof Node) {
			try {
				List<Node> nodes = new ArrayList<Node>();
				for (NodeIterator nit = ((Node) element).getNodes(); nit
						.hasNext();)
					nodes.add(nit.nextNode());
				return nodes.toArray();
			} catch (RepositoryException e) {
				throw new ArgeoException("Cannot get children of " + element, e);
			}
		} else {
			return super.getChildren(element);
		}
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
				throw new ArgeoException("Cannot get parent of " + element, e);
			}
		}
		return super.getParent(element);
	}

	@Override
	public boolean hasChildren(Object element) {
		if (element instanceof Node) {
			Node node = (Node) element;
			try {
				return node.hasNodes();
			} catch (RepositoryException e) {
				throw new ArgeoException("Cannot check whether " + element
						+ " has children", e);
			}
		}
		return super.hasChildren(element);
	}

	public Session getSession() {
		return session;
	}
}
