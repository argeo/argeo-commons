package org.argeo.jcr.ui.explorer.commands;

import java.util.Iterator;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.argeo.ArgeoException;
import org.argeo.eclipse.ui.ErrorFeedback;
import org.argeo.eclipse.ui.TreeParent;
import org.argeo.jcr.ui.explorer.model.SingleJcrNode;
import org.argeo.jcr.ui.explorer.model.WorkspaceNode;
import org.argeo.jcr.ui.explorer.views.GenericJcrBrowser;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
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
		GenericJcrBrowser view = (GenericJcrBrowser) HandlerUtil
				.getActiveWorkbenchWindow(event).getActivePage()
				.findView(HandlerUtil.getActivePartId(event));

		if (selection != null && selection instanceof IStructuredSelection) {
			Iterator<?> it = ((IStructuredSelection) selection).iterator();
			Object obj = null;
			SingleJcrNode ancestor = null;
			WorkspaceNode rootAncestor = null;
			try {
				while (it.hasNext()) {
					obj = it.next();
					if (obj instanceof SingleJcrNode) {
						// Cache objects
						SingleJcrNode sjn = (SingleJcrNode) obj;
						TreeParent tp = (TreeParent) sjn.getParent();
						Node node = sjn.getNode();

						// Jcr Remove
						node.remove();
						node.getSession().save();
						// UI remove
						tp.removeChild(sjn);

						// Check if the parent is the root node
						if (tp instanceof WorkspaceNode)
							rootAncestor = (WorkspaceNode) tp;
						else
							ancestor = getOlder(ancestor, (SingleJcrNode) tp);
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

	private SingleJcrNode getOlder(SingleJcrNode A, SingleJcrNode B) {
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
