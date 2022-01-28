package org.argeo.cms.ui.jcr;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;

import org.argeo.cms.ui.jcr.model.RepositoriesElem;
import org.argeo.cms.ui.jcr.model.RepositoryElem;
import org.argeo.cms.ui.jcr.model.SingleJcrNodeElem;
import org.argeo.cms.ui.jcr.model.WorkspaceElem;
import org.argeo.eclipse.ui.EclipseUiException;
import org.argeo.eclipse.ui.TreeParent;

/** Useful methods to manage the JCR Browser */
public class JcrBrowserUtils {

	public static String getPropertyTypeAsString(Property prop) {
		try {
			return PropertyType.nameFromValue(prop.getType());
		} catch (RepositoryException e) {
			throw new EclipseUiException("Cannot check type for " + prop, e);
		}
	}

	/** Insure that the UI component is not stale, refresh if needed */
	public static void forceRefreshIfNeeded(TreeParent element) {
		Node curNode = null;

		boolean doRefresh = false;

		try {
			if (element instanceof SingleJcrNodeElem) {
				curNode = ((SingleJcrNodeElem) element).getNode();
			} else if (element instanceof WorkspaceElem) {
				curNode = ((WorkspaceElem) element).getRootNode();
			}

			if (curNode != null && element.getChildren().length != curNode.getNodes().getSize())
				doRefresh = true;
			else if (element instanceof RepositoryElem) {
				RepositoryElem rn = (RepositoryElem) element;
				if (rn.isConnected()) {
					String[] wkpNames = rn.getAccessibleWorkspaceNames();
					if (element.getChildren().length != wkpNames.length)
						doRefresh = true;
				}
			} else if (element instanceof RepositoriesElem) {
				doRefresh = true;
				// Always force refresh for RepositoriesElem : the condition
				// below does not take remote repository into account and it is
				// not trivial to do so.

				// RepositoriesElem rn = (RepositoriesElem) element;
				// if (element.getChildren().length !=
				// rn.getRepositoryRegister()
				// .getRepositories().size())
				// doRefresh = true;
			}
			if (doRefresh) {
				element.clearChildren();
				element.getChildren();
			}
		} catch (RepositoryException re) {
			throw new EclipseUiException("Unexpected error while synchronising the UI with the JCR repository", re);
		}
	}
}
