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
package org.argeo.jcr.ui.explorer.commands;

import java.util.Iterator;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.argeo.ArgeoException;
import org.argeo.eclipse.ui.ErrorFeedback;
import org.argeo.eclipse.ui.TreeParent;
import org.argeo.jcr.ui.explorer.model.SingleJcrNodeElem;
import org.argeo.jcr.ui.explorer.model.WorkspaceElem;
import org.argeo.jcr.ui.explorer.views.GenericJcrBrowser;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.handlers.HandlerUtil;

/**
 * Deletes the selected nodes: both in the JCR repository and in the UI view.
 * Warning no check is done, except implementation dependent native checks,
 * handle with care.
 * 
 * This handler is still 'hard linked' to a GenericJcrBrowser view to enable
 * correct tree refresh when a node is added. This must be corrected in future
 * versions.
 */
public class DeleteNodes extends AbstractHandler {
	public Object execute(ExecutionEvent event) throws ExecutionException {
		ISelection selection = HandlerUtil.getActiveWorkbenchWindow(event)
				.getActivePage().getSelection();
		if (selection == null || !(selection instanceof IStructuredSelection))
			return null;

		GenericJcrBrowser view = (GenericJcrBrowser) HandlerUtil
				.getActiveWorkbenchWindow(event).getActivePage()
				.findView(HandlerUtil.getActivePartId(event));

		// confirmation
		StringBuffer buf = new StringBuffer("");
		Iterator<?> lst = ((IStructuredSelection) selection).iterator();
		while (lst.hasNext()) {
			SingleJcrNodeElem sjn = ((SingleJcrNodeElem) lst.next());
			buf.append(sjn.getName()).append(' ');
		}
		Boolean ok = MessageDialog.openConfirm(
				HandlerUtil.getActiveShell(event), "Confirm deletion",
				"Do you want to delete " + buf + "?");

		// operation
		if (ok) {
			Iterator<?> it = ((IStructuredSelection) selection).iterator();
			Object obj = null;
			SingleJcrNodeElem ancestor = null;
			WorkspaceElem rootAncestor = null;
			try {
				while (it.hasNext()) {
					obj = it.next();
					if (obj instanceof SingleJcrNodeElem) {
						// Cache objects
						SingleJcrNodeElem sjn = (SingleJcrNodeElem) obj;
						TreeParent tp = (TreeParent) sjn.getParent();
						Node node = sjn.getNode();

						// Jcr Remove
						node.remove();
						node.getSession().save();
						// UI remove
						tp.removeChild(sjn);

						// Check if the parent is the root node
						if (tp instanceof WorkspaceElem)
							rootAncestor = (WorkspaceElem) tp;
						else
							ancestor = getOlder(ancestor, (SingleJcrNodeElem) tp);
					}
				}
				if (rootAncestor != null)
					view.nodeRemoved(rootAncestor);
				else if (ancestor != null)
					view.nodeRemoved(ancestor);
			} catch (Exception e) {
				ErrorFeedback.show("Cannot delete selected node ", e);
			}
		}
		return null;
	}

	private SingleJcrNodeElem getOlder(SingleJcrNodeElem A, SingleJcrNodeElem B) {
		try {
			if (A == null)
				return B == null ? null : B;
			// Todo enhanced this method
			else
				return A.getNode().getDepth() <= B.getNode().getDepth() ? A : B;
		} catch (RepositoryException re) {
			throw new ArgeoException("Cannot find ancestor", re);
		}
	}
}
