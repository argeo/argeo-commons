package org.argeo.jcr.ui.explorer.browser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.argeo.ArgeoException;
import org.argeo.jcr.JcrUtils;
import org.argeo.jcr.RepositoryRegister;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

/**
 * Implementation of the {@code ITreeContentProvider} to display multiple
 * repository environment in a tree like structure
 * 
 */
public class NodeContentProvider implements ITreeContentProvider {
	private ItemComparator itemComparator = new ItemComparator();

	private RepositoryRegister repositoryRegister;
	private Session userSession;

	public NodeContentProvider(Session userSession,
			RepositoryRegister repositoryRegister) {
		this.userSession = userSession;
		this.repositoryRegister = repositoryRegister;
	}

	/**
	 * Sends back the first level of the Tree. Independent from inputElement
	 * that can be null
	 */
	public Object[] getElements(Object inputElement) {
		List<Object> objs = new ArrayList<Object>();
		if (userSession != null) {
			Node userHome = JcrUtils.getUserHome(userSession);
			if (userHome != null)
				objs.add(userHome);
		}
		if (repositoryRegister != null)
			objs.add(repositoryRegister);
		return objs.toArray();
	}

	public Object[] getChildren(Object parentElement) {
		if (parentElement instanceof Node) {
			return childrenNodes((Node) parentElement);
		} else if (parentElement instanceof RepositoryNode) {
			return ((RepositoryNode) parentElement).getChildren();
		} else if (parentElement instanceof WorkspaceNode) {
			Session session = ((WorkspaceNode) parentElement).getSession();
			if (session == null)
				return new Object[0];

			try {
				return childrenNodes(session.getRootNode());
			} catch (RepositoryException e) {
				throw new ArgeoException("Cannot retrieve root node of "
						+ session, e);
			}
		} else if (parentElement instanceof RepositoryRegister) {
			RepositoryRegister repositoryRegister = (RepositoryRegister) parentElement;
			List<RepositoryNode> nodes = new ArrayList<RepositoryNode>();
			Map<String, Repository> repositories = repositoryRegister
					.getRepositories();
			for (String name : repositories.keySet()) {
				nodes.add(new RepositoryNode(name, repositories.get(name)));
			}
			return nodes.toArray();
		} else {
			return new Object[0];
		}
	}

	public Object getParent(Object element) {
		try {
			if (element instanceof Node) {
				Node node = (Node) element;
				if (!node.getPath().equals("/"))
					return node.getParent();
				else
					return null;
			}
			return null;
		} catch (RepositoryException e) {
			return null;
		}
	}

	public boolean hasChildren(Object element) {
		try {
			if (element instanceof Node) {
				return ((Node) element).hasNodes();
			} else if (element instanceof RepositoryNode) {
				return ((RepositoryNode) element).hasChildren();
			} else if (element instanceof WorkspaceNode) {
				return ((WorkspaceNode) element).getSession() != null;
			} else if (element instanceof RepositoryRegister) {
				return ((RepositoryRegister) element).getRepositories().size() > 0;
			}
			return false;
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
