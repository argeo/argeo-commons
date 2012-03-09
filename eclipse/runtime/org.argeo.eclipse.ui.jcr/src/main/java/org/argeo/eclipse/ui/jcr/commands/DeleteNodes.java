/*
 * Copyright (C) 2007-2012 Mathieu Baudier
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
package org.argeo.eclipse.ui.jcr.commands;

import java.util.Iterator;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.argeo.ArgeoException;
import org.argeo.eclipse.ui.ErrorFeedback;
import org.argeo.eclipse.ui.jcr.views.AbstractJcrBrowser;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.handlers.HandlerUtil;

/**
 * Deletes the selected nodes and refresh the corresponding AbstractJcrView.
 * Note that no model specific check is done to see if the node can be removed
 * or not. Extend or override to implement specific behaviour.
 */
public class DeleteNodes extends AbstractHandler {
	public final static String ID = "org.argeo.eclipse.ui.jcr.deleteNodes";
	public final static String DEFAULT_LABEL = "Delete selected nodes";

	public Object execute(ExecutionEvent event) throws ExecutionException {
		ISelection selection = HandlerUtil.getActiveWorkbenchWindow(event)
				.getActivePage().getSelection();
		AbstractJcrBrowser view = (AbstractJcrBrowser) HandlerUtil
				.getActiveWorkbenchWindow(event).getActivePage()
				.findView(HandlerUtil.getActivePartId(event));

		if (selection != null && selection instanceof IStructuredSelection) {
			Iterator<?> it = ((IStructuredSelection) selection).iterator();
			Object obj = null;
			Node ancestor = null;
			try {
				while (it.hasNext()) {
					obj = it.next();
					if (obj instanceof Node) {
						Node node = (Node) obj;
						Node parentNode = node.getParent();
						node.remove();
						node.getSession().save();
						ancestor = getOlder(ancestor, parentNode);
					}
				}
				if (ancestor != null)
					view.nodeRemoved(ancestor);
			} catch (Exception e) {
				ErrorFeedback.show("Cannot delete node " + obj, e);
			}
		}
		return null;
	}

	protected Node getOlder(Node A, Node B) {
		try {

			if (A == null)
				return B == null ? null : B;
			// Todo enhanced this method
			else
				return A.getDepth() <= B.getDepth() ? A : B;
		} catch (RepositoryException re) {
			throw new ArgeoException("Cannot find ancestor", re);
		}
	}
}
