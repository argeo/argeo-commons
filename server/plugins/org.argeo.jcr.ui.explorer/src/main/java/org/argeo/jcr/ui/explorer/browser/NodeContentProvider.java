package org.argeo.jcr.ui.explorer.browser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.Session;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.argeo.eclipse.ui.TreeParent;
import org.argeo.jcr.ArgeoJcrConstants;
import org.argeo.jcr.JcrUtils;
import org.argeo.jcr.RepositoryRegister;
import org.argeo.jcr.security.JcrKeyring;
import org.argeo.jcr.ui.explorer.model.RepositoriesNode;
import org.argeo.jcr.ui.explorer.model.SingleJcrNode;
import org.argeo.jcr.ui.explorer.utils.TreeObjectsComparator;
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
	final private RepositoryRegister repositoryRegister;
	final private Session userSession;
	final private JcrKeyring jcrKeyring;
	final private boolean sortChildren;

	// Utils
	private TreeObjectsComparator itemComparator = new TreeObjectsComparator();

	public NodeContentProvider(JcrKeyring jcrKeyring,
			RepositoryRegister repositoryRegister, Boolean sortChildren) {
		this.userSession = jcrKeyring != null ? jcrKeyring.getSession() : null;
		this.jcrKeyring = jcrKeyring;
		this.repositoryRegister = repositoryRegister;
		this.sortChildren = sortChildren;
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
					null, jcrKeyring));
		return objs.toArray();
	}

	public Object[] getChildren(Object parentElement) {
		if (parentElement instanceof TreeParent) {
			if (sortChildren) {
				// TreeParent[] arr = (TreeParent[]) ((TreeParent)
				// parentElement)
				// .getChildren();
				Object[] tmpArr = ((TreeParent) parentElement).getChildren();
				TreeParent[] arr = new TreeParent[tmpArr.length];
				for (int i = 0; i < tmpArr.length; i++)
					arr[i] = (TreeParent) tmpArr[i];

				Arrays.sort(arr, itemComparator);
				return arr;
			} else
				return ((TreeParent) parentElement).getChildren();

		} else {
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
		}
		return false;
	}

	public void dispose() {
	}

	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
	}
}
