package org.argeo.eclipse.ui.jcr.commands;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeType;

import org.argeo.eclipse.ui.ErrorFeedback;
import org.argeo.eclipse.ui.dialogs.SingleValue;
import org.argeo.eclipse.ui.jcr.views.AbstractJcrBrowser;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.handlers.HandlerUtil;

/** Adds a node of type nt:folder */
public class AddFileFolder extends AbstractHandler {

	public Object execute(ExecutionEvent event) throws ExecutionException {
		ISelection selection = HandlerUtil.getActiveWorkbenchWindow(event)
				.getActivePage().getSelection();
		AbstractJcrBrowser view = (AbstractJcrBrowser) HandlerUtil
				.getActiveWorkbenchWindow(event).getActivePage()
				.findView(HandlerUtil.getActivePartId(event));
		if (selection != null && !selection.isEmpty()
				&& selection instanceof IStructuredSelection) {
			Object obj = ((IStructuredSelection) selection).getFirstElement();
			if (obj instanceof Node) {
				String folderName = SingleValue.ask("Folder name",
						"Enter folder name");
				if (folderName != null) {
					Node parentNode = (Node) obj;
					try {
						Node newNode = parentNode.addNode(folderName,
								NodeType.NT_FOLDER);
						view.nodeAdded(parentNode, newNode);
						parentNode.getSession().save();
					} catch (RepositoryException e) {
						ErrorFeedback.show("Cannot create folder " + folderName
								+ " under " + parentNode, e);
					}
				}
			} else {
				ErrorFeedback.show("Can only add file folder to a node");
			}
		}
		return null;
	}

}
