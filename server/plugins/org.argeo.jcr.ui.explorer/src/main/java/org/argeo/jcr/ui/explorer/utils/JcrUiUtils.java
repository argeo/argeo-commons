/*
 * Copyright (C) 2007-2012 Argeo GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.argeo.jcr.ui.explorer.utils;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.argeo.ArgeoException;
import org.argeo.eclipse.ui.TreeParent;
import org.argeo.jcr.ui.explorer.model.RepositoriesNode;
import org.argeo.jcr.ui.explorer.model.RepositoryNode;
import org.argeo.jcr.ui.explorer.model.SingleJcrNode;
import org.argeo.jcr.ui.explorer.model.WorkspaceNode;

/** Centralizes some useful methods to build UIs with JCR */
public class JcrUiUtils {

	/** Insure that the UI component is not stale, refresh if needed */
	public static void forceRefreshIfNeeded(TreeParent element) {
		Node curNode = null;

		boolean doRefresh = false;

		try {
			if (element instanceof SingleJcrNode) {
				curNode = ((SingleJcrNode) element).getNode();
			} else if (element instanceof WorkspaceNode) {
				curNode = ((WorkspaceNode) element).getRootNode();
			}

			if (curNode != null
					&& element.getChildren().length != curNode.getNodes()
							.getSize())
				doRefresh = true;
			else if (element instanceof RepositoryNode) {
				RepositoryNode rn = (RepositoryNode) element;
				if (rn.isConnected()) {
					String[] wkpNames = rn.getAccessibleWorkspaceNames();
					if (element.getChildren().length != wkpNames.length)
						doRefresh = true;
				}
			} else if (element instanceof RepositoriesNode) {
				RepositoriesNode rn = (RepositoriesNode) element;
				if (element.getChildren().length != rn.getRepositoryRegister()
						.getRepositories().size())
					doRefresh = true;
			}
			if (doRefresh) {
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
