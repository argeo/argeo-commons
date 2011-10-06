package org.argeo.jcr.ui.explorer.utils;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.argeo.ArgeoException;
import org.argeo.eclipse.ui.TreeParent;
import org.argeo.jcr.ui.explorer.model.SingleJcrNode;
import org.argeo.jcr.ui.explorer.model.WorkspaceNode;

/** Centralizes some useful methods to build Uis with JCR */
public class JcrUiUtils {

	/** Insure that the UI componant is not stale, refresh if needed */
	public static void forceRefreshIfNeeded(TreeParent element) {
		Node curNode;

		if (element instanceof SingleJcrNode)
			curNode = ((SingleJcrNode) element).getNode();
		else if (element instanceof WorkspaceNode)
			curNode = ((WorkspaceNode) element).getRootNode();
		else
			return;
		// TODO implement specific methods for other cases

		try {
			// we mainly rely on nb of children
			if (element.getChildren().length == curNode.getNodes().getSize())
				return;
			else {
				// get rid of children of UI object
				element.clearChildren();
				element.getChildren();
			}
		} catch (RepositoryException re) {
			throw new ArgeoException(
					"Unexpected error while synchronising the UI with the JCR repository",
					re);
		}
	}

	/**
	 * Insure that a model element is inline with the underlying data by
	 * cleaning the corresponding subtree and building it again.
	 */
	public static void forceRebuild(TreeParent element) {
		// TODO implement this method if needed.
	}
	/**
	 * Workaround to get the alias of the repository that contains the given
	 * element. As we cannot browse the UI tree upward we recursively browse it
	 * downward until we find the given element
	 * */
	// public static String getRepositoryAliasFromITreeElement(
	// NodeContentProvider ncp, Object element) {
	// RepositoryNode repositoryNode = null;
	// if (element instanceof RepositoryNode)
	// return ((RepositoryNode) element).getName();
	// else if (element instanceof RepositoryRegister)
	// throw new ArgeoException(
	// "Cannot get alias for a repository register");
	//
	// // Get root elements
	// Object[] elements = ncp.getElements(null);
	//
	// try {
	// for (int i = 0; i < elements.length; i++) {
	// if (elements[i] instanceof Node) {
	// Node curNode = (Node) elements[i];
	// if (curNode.isNodeType(ArgeoTypes.ARGEO_USER_HOME)) {
	// // Do nothing, we'll find the node in the "normal" tree
	// // and
	// // get corresponding alias this way round
	// } else
	// throw new ArgeoException(
	// "Normal nodes should not be at the root of NodeTreeViewer");
	// } else if (elements[i] instanceof RepositoryRegister) {
	// RepositoryRegister repositoryRegister = (RepositoryRegister) elements[i];
	// Map<String, Repository> repositories = repositoryRegister
	// .getRepositories();
	//
	// for (String name : repositories.keySet()) {
	// boolean found = isElementInCurrentTreePart(
	// ncp,
	// new RepositoryNode(name, repositories.get(name)),
	// (Node) element);
	// if (found)
	// return name;
	// }
	// } else
	// throw new ArgeoException(
	// "Unexpected object class at the root of NodeTreeViewer");
	// }
	// } catch (RepositoryException re) {
	// throw new ArgeoException(
	// "Unexpected error while retrieving Alias name", re);
	// }
	// return null;
	// }
	//
	// /** implements the recursivity */
	// private static boolean isElementInCurrentTreePart(NodeContentProvider
	// ncp,
	// Object parentElement, NodParente searchedElement) {
	// boolean found = false;
	// if (parentElement instanceof WorkspaceNode) {
	// WorkspaceNode wn = (WorkspaceNode) parentElement;
	// Object[] children = wn.getChildren();
	// int i = children.length - 1;
	// while (!found && i >= 0) {
	// found = isElementInCurrentTreePart(ncp, children[i],
	// searchedElement);
	// }
	// return found;
	// } else if (parentElement instanceof RepositoryNode) {
	// RepositoryNode rn = (RepositoryNode) parentElement;
	// Object[] children = rn.getChildren();
	// int i = children.length - 1;
	// while (!found && i >= 0) {
	// found = isElementInCurrentTreePart(ncp, children[i],
	// searchedElement);
	// }
	// return found;
	// } else {
	// Node node = (Node) parentElement;
	// if (node.equals(searchedElement))
	// return true;
	// NodeIterator ni;
	// try {
	// ni = node.getNodes();
	// while (!found && ni.hasNext()) {
	// found = isElementInCurrentTreePart(ncp, ni.nextNode(),
	// searchedElement);
	// }
	// } catch (RepositoryException e) {
	// throw new ArgeoException("unexpected erreur while recursively"
	// + " recovering RepositoryNode for selected object", e);
	// }
	//
	// return found;
	// }
	// }
}
