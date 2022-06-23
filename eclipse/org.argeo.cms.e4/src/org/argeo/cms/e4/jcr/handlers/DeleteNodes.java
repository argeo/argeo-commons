package org.argeo.cms.e4.jcr.handlers;

import java.util.List;

import javax.inject.Named;
import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.argeo.cms.e4.jcr.JcrBrowserView;
import org.argeo.cms.ui.jcr.model.SingleJcrNodeElem;
import org.argeo.cms.ui.jcr.model.WorkspaceElem;
import org.argeo.cms.ux.widgets.TreeParent;
import org.argeo.eclipse.ui.EclipseUiException;
import org.argeo.eclipse.ui.dialogs.ErrorFeedback;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.services.IServiceConstants;
import org.eclipse.e4.ui.workbench.modeling.ESelectionService;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;

/**
 * Delete the selected nodes: both in the JCR repository and in the UI view.
 * Warning no check is done, except implementation dependent native checks,
 * handle with care.
 * 
 * This handler is still 'hard linked' to a GenericJcrBrowser view to enable
 * correct tree refresh when a node is added. This must be corrected in future
 * versions.
 */
public class DeleteNodes {
	@Execute
	public void execute(@Named(IServiceConstants.ACTIVE_PART) MPart part, ESelectionService selectionService) {
		List<?> selection = (List<?>) selectionService.getSelection();
		if (selection == null)
			return;

		JcrBrowserView view = (JcrBrowserView) part.getObject();

		// confirmation
		StringBuffer buf = new StringBuffer("");
		for (Object o : selection) {
			SingleJcrNodeElem sjn = (SingleJcrNodeElem) o;
			buf.append(sjn.getName()).append(' ');
		}
		Boolean doRemove = MessageDialog.openConfirm(Display.getCurrent().getActiveShell(), "Confirm deletion",
				"Do you want to delete " + buf + "?");

		// operation
		if (doRemove) {
			SingleJcrNodeElem ancestor = null;
			WorkspaceElem rootAncestor = null;
			try {
				for (Object obj : selection) {
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
	}

	private SingleJcrNodeElem getOlder(SingleJcrNodeElem A, SingleJcrNodeElem B) {
		try {
			if (A == null)
				return B == null ? null : B;
			// Todo enhanced this method
			else
				return A.getNode().getDepth() <= B.getNode().getDepth() ? A : B;
		} catch (RepositoryException re) {
			throw new EclipseUiException("Cannot find ancestor", re);
		}
	}
}
