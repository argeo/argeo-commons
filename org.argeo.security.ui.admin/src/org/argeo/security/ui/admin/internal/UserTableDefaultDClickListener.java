package org.argeo.security.ui.admin.internal;

import org.argeo.ArgeoException;
import org.argeo.eclipse.ui.workbench.WorkbenchUiPlugin;
import org.argeo.security.ui.admin.editors.UserEditor;
import org.argeo.security.ui.admin.editors.UserEditorInput;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
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
		// IWorkbench iw =
		IWorkbenchWindow iww = WorkbenchUiPlugin.getDefault().getWorkbench()
				.getActiveWorkbenchWindow();
		IWorkbenchPage iwp = iww.getActivePage();
		UserEditorInput uei = new UserEditorInput(user.getName());

		try {
			// IEditorPart editor =
			iwp.openEditor(uei, UserEditor.ID);
		} catch (PartInitException pie) {
			throw new ArgeoException("Unable to open UserEditor for " + user,
					pie);
		}
	}
}