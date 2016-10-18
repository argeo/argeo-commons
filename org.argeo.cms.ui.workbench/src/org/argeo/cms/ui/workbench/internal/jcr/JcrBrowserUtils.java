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
package org.argeo.cms.ui.workbench.internal.jcr;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.argeo.cms.ui.workbench.internal.jcr.model.RepositoriesElem;
import org.argeo.cms.ui.workbench.internal.jcr.model.RepositoryElem;
import org.argeo.cms.ui.workbench.internal.jcr.model.SingleJcrNodeElem;
import org.argeo.cms.ui.workbench.internal.jcr.model.WorkspaceElem;
import org.argeo.eclipse.ui.EclipseUiException;
import org.argeo.eclipse.ui.TreeParent;

/** Useful methods to manage the JCR Browser */
public class JcrBrowserUtils {

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

			if (curNode != null
					&& element.getChildren().length != curNode.getNodes()
							.getSize())
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
			throw new EclipseUiException(
					"Unexpected error while synchronising the UI with the JCR repository",
					re);
		}
	}
}
