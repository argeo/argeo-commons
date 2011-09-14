package org.argeo.jcr.ui.explorer.commands;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeType;

import org.argeo.eclipse.ui.ErrorFeedback;
import org.argeo.eclipse.ui.dialogs.SingleValue;
import org.argeo.eclipse.ui.jcr.JcrUiPlugin;
import org.argeo.jcr.ui.explorer.model.SingleJcrNode;
import org.argeo.jcr.ui.explorer.views.GenericJcrBrowser;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.handlers.HandlerUtil;

/** Adds a node of type nt:folder */
public class AddFolderNode extends AbstractHandler {

	public Object execute(ExecutionEvent event) throws ExecutionException {
		ISelection selection = HandlerUtil.getActiveWorkbenchWindow(event)
				.getActivePage().getSelection();
		GenericJcrBrowser view = (GenericJcrBrowser) HandlerUtil
				.getActiveWorkbenchWindow(event).getActivePage()
				.findView(HandlerUtil.getActivePartId(event));
		if (selection != null && !selection.isEmpty()
				&& selection instanceof IStructuredSelection) {
			Object obj = ((IStructuredSelection) selection).getFirstElement();

			if (obj instanceof SingleJcrNode) {
				String folderName = SingleValue.ask("Folder name",
						"Enter folder name");
				if (folderName != null) {
					SingleJcrNode sjn = (SingleJcrNode) obj;
					Node parentNode = sjn.getNode();
					try {
						Node newNode = parentNode.addNode(folderName,
								NodeType.NT_FOLDER);
						parentNode.getSession().save();
						view.nodeAdded(sjn);
					} catch (RepositoryException e) {
						ErrorFeedback.show("Cannot create folder " + folderName
								+ " under " + parentNode, e);
					}
				}
			} else {
				ErrorFeedback.show(JcrUiPlugin
						.getMessage("errorUnvalidNtFolderNodeType"));
			}
		}
		return null;
	}

}
