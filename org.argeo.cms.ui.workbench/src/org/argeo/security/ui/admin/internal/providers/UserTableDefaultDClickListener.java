package org.argeo.security.ui.admin.internal.providers;

import org.argeo.cms.CmsException;
import org.argeo.cms.ui.workbench.SecurityUiPlugin;
import org.argeo.security.ui.admin.internal.parts.UserEditor;
import org.argeo.security.ui.admin.internal.parts.UserEditorInput;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.osgi.service.useradmin.Group;
import org.osgi.service.useradmin.User;

/**
 * Default double click listener for the various user tables, will open the
 * clicked item in the editor
 */
public class UserTableDefaultDClickListener implements IDoubleClickListener {
	public void doubleClick(DoubleClickEvent evt) {
		if (evt.getSelection().isEmpty())
			return;
		Object obj = ((IStructuredSelection) evt.getSelection())
				.getFirstElement();
		User user = (User) obj;
		IWorkbenchWindow iww = SecurityUiPlugin.getDefault().getWorkbench()
				.getActiveWorkbenchWindow();
		IWorkbenchPage iwp = iww.getActivePage();
		UserEditorInput uei = new UserEditorInput(user.getName());

		try {
			// Works around the fact that dynamic setting of the editor icon
			// causes NPE after a login/logout on RAP
			if (user instanceof Group)
				iwp.openEditor(uei, UserEditor.GROUP_EDITOR_ID);
			else
				iwp.openEditor(uei, UserEditor.USER_EDITOR_ID);
		} catch (PartInitException pie) {
			throw new CmsException("Unable to open UserEditor for " + user, pie);
		}
	}
}