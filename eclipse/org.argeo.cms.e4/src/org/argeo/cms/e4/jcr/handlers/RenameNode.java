package org.argeo.cms.e4.jcr.handlers;

import java.util.List;

import javax.inject.Named;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.argeo.cms.e4.jcr.JcrBrowserView;
import org.argeo.cms.ui.jcr.model.SingleJcrNodeElem;
import org.argeo.eclipse.ui.EclipseUiException;
import org.argeo.eclipse.ui.dialogs.SingleValue;
import org.argeo.jcr.JcrUtils;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.services.IServiceConstants;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.e4.ui.workbench.modeling.ESelectionService;

/**
 * Canonically call JCR Session#move(String, String) on the first element
 * returned by HandlerUtil#getActiveWorkbenchWindow()
 * (...getActivePage().getSelection()), if it is a {@link SingleJcrNodeElem}.
 * The user must then fill a new name in and confirm
 */
public class RenameNode {
	@Execute
	public void execute(@Named(IServiceConstants.ACTIVE_PART) MPart part, EPartService partService,
			ESelectionService selectionService) {
		List<?> selection = (List<?>) selectionService.getSelection();
		if (selection == null || selection.size() != 1)
			return;
		JcrBrowserView view = (JcrBrowserView) part.getObject();

		Object element = selection.get(0);
		if (element instanceof SingleJcrNodeElem) {
			SingleJcrNodeElem sjn = (SingleJcrNodeElem) element;
			Node node = sjn.getNode();
			Session session = null;
			String newName = null;
			String oldPath = null;
			try {
				newName = SingleValue.ask("New node name", "Please provide a new name for [" + node.getName() + "]");
				// TODO sanity check and user feedback
				newName = JcrUtils.replaceInvalidChars(newName);
				oldPath = node.getPath();
				session = node.getSession();
				session.move(oldPath, JcrUtils.parentPath(oldPath) + "/" + newName);
				session.save();

				// Manually refresh the browser view. Must be enhanced
				view.refresh(sjn);
			} catch (RepositoryException e) {
				throw new EclipseUiException("Unable to rename " + node + " to " + newName, e);
			}
		}
	}
}
