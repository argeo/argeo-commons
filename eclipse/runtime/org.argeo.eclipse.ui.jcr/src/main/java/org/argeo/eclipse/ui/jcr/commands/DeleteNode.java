package org.argeo.eclipse.ui.jcr.commands;

import java.util.Iterator;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.argeo.ArgeoException;
import org.argeo.eclipse.ui.dialogs.Error;
import org.argeo.eclipse.ui.jcr.views.AbstractJcrBrowser;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.handlers.HandlerUtil;

/** Deletes the selected nodes */
public class DeleteNode extends AbstractHandler {
	private final static Log log = LogFactory.getLog(DeleteNode.class);

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
				Error.show("Cannot delete node " + obj, e);
			}
		}
		return null;
	}

	private Node getOlder(Node A, Node B) {
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
