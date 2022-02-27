package org.argeo.cms.e4.users.handlers;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.argeo.cms.auth.UserAdminUtils;
import org.argeo.cms.e4.users.UserAdminWrapper;
import org.argeo.cms.e4.users.UsersView;
import org.eclipse.e4.core.di.annotations.CanExecute;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.services.IServiceConstants;
import org.eclipse.e4.ui.workbench.modeling.ESelectionService;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.osgi.service.useradmin.User;
import org.osgi.service.useradmin.UserAdmin;
import org.osgi.service.useradmin.UserAdminEvent;

/** Delete the selected users */
public class DeleteUsers {
	// public final static String ID = WorkbenchUiPlugin.PLUGIN_ID + ".deleteUsers";

	/* DEPENDENCY INJECTION */
	@Inject
	private UserAdminWrapper userAdminWrapper;

	@SuppressWarnings("unchecked")
	@Execute
	public void execute(@Named(IServiceConstants.ACTIVE_PART) MPart part, ESelectionService selectionService) {
		// ISelection selection = null;// HandlerUtil.getCurrentSelection(event);
		// if (selection.isEmpty())
		// return null;
		List<User> selection = (List<User>) selectionService.getSelection();
		if (selection == null)
			return;

//		Iterator<User> it = ((IStructuredSelection) selection).iterator();
//		List<User> users = new ArrayList<User>();
		StringBuilder builder = new StringBuilder();

		for(User user:selection) {
			User currUser = user;
//			User currUser = it.next();
			String userName = UserAdminUtils.getUserLocalId(currUser.getName());
			if (UserAdminUtils.isCurrentUser(currUser)) {
				MessageDialog.openError(Display.getCurrent().getActiveShell(), "Deletion forbidden",
						"You cannot delete your own user this way.");
				return;
			}
			builder.append(userName).append("; ");
//			users.add(currUser);
		}

		if (!MessageDialog.openQuestion(Display.getCurrent().getActiveShell(), "Delete Users",
				"Are you sure that you want to delete these users?\n" + builder.substring(0, builder.length() - 2)))
			return;

		userAdminWrapper.beginTransactionIfNeeded();
		UserAdmin userAdmin = userAdminWrapper.getUserAdmin();
		// IWorkbenchPage iwp =
		// HandlerUtil.getActiveWorkbenchWindow(event).getActivePage();

		for (User user : selection) {
			String userName = user.getName();
			// TODO find a way to close the editor cleanly if opened. Cannot be
			// done through the UserAdminListeners, it causes a
			// java.util.ConcurrentModificationException because disposing the
			// editor unregisters and disposes the listener
			// IEditorPart part = iwp.findEditor(new UserEditorInput(userName));
			// if (part != null)
			// iwp.closeEditor(part, false);
			userAdmin.removeRole(userName);
		}
		userAdminWrapper.commitOrNotifyTransactionStateChange();

		for (User user : selection) {
			userAdminWrapper.notifyListeners(new UserAdminEvent(null, UserAdminEvent.ROLE_REMOVED, user));
		}
	}

	@CanExecute
	public boolean canExecute(@Named(IServiceConstants.ACTIVE_PART) MPart part, ESelectionService selectionService) {
		return part.getObject() instanceof UsersView && selectionService.getSelection() != null;
	}
}
