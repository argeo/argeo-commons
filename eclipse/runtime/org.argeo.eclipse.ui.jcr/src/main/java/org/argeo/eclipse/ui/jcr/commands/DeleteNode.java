package org.argeo.eclipse.ui.jcr.commands;

import java.util.Iterator;

import javax.jcr.Node;

import org.argeo.eclipse.ui.dialogs.Error;
import org.argeo.eclipse.ui.jcr.views.GenericJcrBrowser;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.handlers.HandlerUtil;

/** Deletes the selected nodes */
public class DeleteNode extends AbstractHandler {

	public Object execute(ExecutionEvent event) throws ExecutionException {
		ISelection selection = HandlerUtil.getActiveWorkbenchWindow(event)
				.getActivePage().getSelection();
		GenericJcrBrowser view = (GenericJcrBrowser) HandlerUtil
				.getActiveWorkbenchWindow(event).getActivePage()
				.findView(HandlerUtil.getActivePartId(event));
		if (selection != null && selection instanceof IStructuredSelection) {
			Iterator<?> it = ((IStructuredSelection) selection).iterator();

			Object obj = null;
			try {
				while (it.hasNext()) {
					obj = it.next();
					if (obj instanceof Node) {
						Node node = (Node) obj;
						Node parentNode = node.getParent();
						node.remove();
						view.nodeRemoved(parentNode);
						node.getSession().save();
					}
				}
			} catch (Exception e) {
				Error.show("Cannot delete node " + obj, e);
			}
		}
		return null;
	}

}
