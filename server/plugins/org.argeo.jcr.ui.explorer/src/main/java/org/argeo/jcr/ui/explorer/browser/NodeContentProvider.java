package org.argeo.jcr.ui.explorer.browser;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.Session;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.argeo.eclipse.ui.TreeParent;
import org.argeo.jcr.ArgeoJcrConstants;
import org.argeo.jcr.JcrUtils;
import org.argeo.jcr.RepositoryRegister;
import org.argeo.jcr.ui.explorer.model.RepositoriesNode;
import org.argeo.jcr.ui.explorer.model.SingleJcrNode;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

/**
 * Implementation of the {@code ITreeContentProvider} to display multiple
 * repository environment in a tree like structure
 * 
 */
public class NodeContentProvider implements ITreeContentProvider {
	private final static Log log = LogFactory.getLog(NodeContentProvider.class);

	// Business Objects
	private RepositoryRegister repositoryRegister;
	private Session userSession;

	// Utils
	// private ItemComparator itemComparator = new ItemComparator();

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
				// TODO : find a way to dynamically get alias for the node
				objs.add(new SingleJcrNode(null, userHome, userSession
						.getUserID(), ArgeoJcrConstants.ALIAS_NODE));
		}
		if (repositoryRegister != null)
			objs.add(new RepositoriesNode("Repositories", repositoryRegister,
					null));
		return objs.toArray();
	}

	public Object[] getChildren(Object parentElement) {
		// if (parentElement instanceof Node) {
		// return childrenNodes((Node) parentElement);
		// } else if (parentElement instanceof RepositoryNode) {
		// return ((RepositoryNode) parentElement).getChildren();
		// } else if (parentElement instanceof WorkspaceNode) {
		// Session session = ((WorkspaceNode) parentElement).getSession();
		// if (session == null)
		// return new Object[0];
		//
		// try {
		// return childrenNodes(session.getRootNode());
		// } catch (RepositoryException e) {
		// throw new ArgeoException("Cannot retrieve root node of "
		// + session, e);
		// }
		// } else if (parentElement instanceof RepositoryRegister) {
		// RepositoryRegister repositoryRegister = (RepositoryRegister)
		// parentElement;
		// List<RepositoryNode> nodes = new ArrayList<RepositoryNode>();
		// Map<String, Repository> repositories = repositoryRegister
		// .getRepositories();
		// for (String name : repositories.keySet()) {
		// nodes.add(new RepositoryNode(name, repositories.get(name)));
		// }
		// return nodes.toArray();

		if (parentElement instanceof TreeParent)
			return ((TreeParent) parentElement).getChildren();
		else {
			return new Object[0];
		}
	}

	public Object getParent(Object element) {
		if (element instanceof TreeParent) {
			return ((TreeParent) element).getParent();
		} else
			return null;
	}

	public boolean hasChildren(Object element) {
		if (element instanceof RepositoriesNode) {
			RepositoryRegister rr = ((RepositoriesNode) element)
					.getRepositoryRegister();
			return rr.getRepositories().size() > 0;
		} else if (element instanceof TreeParent) {
			TreeParent tp = (TreeParent) element;
			return tp.hasChildren();
			// } else if (element instanceof RepositoryNode) {
			// return ((RepositoryNode) element).hasChildren();
			// } else if (element instanceof WorkspaceNode) {
			// return ((WorkspaceNode) element).getSession() != null;
		}
		return false;
		// } catch (RepositoryException e) {
		// throw new ArgeoException("Cannot check children of " + element, e);
		// }
	}

	public void dispose() {
	}

	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
	}

	// protected Object[] childrenNodes(Node parentNode) {
	// try {
	// List<Node> children = new ArrayList<Node>();
	// NodeIterator nit = parentNode.getNodes();
	// while (nit.hasNext()) {
	// Node node = nit.nextNode();
	// children.add(node);
	// }
	// Node[] arr = children.toArray(new Node[children.size()]);
	// Arrays.sort(arr, itemComparator);
	// return arr;
	// } catch (RepositoryException e) {
	// throw new ArgeoException("Cannot list children of " + parentNode, e);
	// }
	// }

}
