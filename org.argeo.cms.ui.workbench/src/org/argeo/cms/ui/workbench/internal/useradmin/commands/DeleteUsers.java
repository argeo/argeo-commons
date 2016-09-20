/*
 * Copyright (C) 2007-2012 Argeo GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.argeo.cms.ui.workbench.internal.useradmin.commands;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.argeo.cms.ui.workbench.WorkbenchUiPlugin;
import org.argeo.cms.ui.workbench.internal.useradmin.UserAdminWrapper;
import org.argeo.cms.ui.workbench.internal.useradmin.parts.UserEditorInput;
import org.argeo.cms.util.useradmin.UserAdminUtils;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.handlers.HandlerUtil;
import org.osgi.service.useradmin.User;
import org.osgi.service.useradmin.UserAdmin;
import org.osgi.service.useradmin.UserAdminEvent;

/** Delete the selected users */
public class DeleteUsers extends AbstractHandler {
	public final static String ID = WorkbenchUiPlugin.PLUGIN_ID + ".deleteUsers";

	/* DEPENDENCY INJECTION */
	private UserAdminWrapper userAdminWrapper;

	@SuppressWarnings("unchecked")
	public Object execute(ExecutionEvent event) throws ExecutionException {
		ISelection selection = HandlerUtil.getCurrentSelection(event);
		if (selection.isEmpty())
			return null;

		Iterator<User> it = ((IStructuredSelection) selection).iterator();
		List<User> users = new ArrayList<User>();
		StringBuilder builder = new StringBuilder();

		while (it.hasNext()) {
			User currUser = it.next();
			String userName = UserAdminUtils.getUsername(currUser);
			if (UserAdminUtils.isCurrentUser(currUser)) {
				MessageDialog.openError(HandlerUtil.getActiveShell(event),
						"Deletion forbidden",
						"You cannot delete your own user this way.");
				return null;
			}
			builder.append(userName).append("; ");
			users.add(currUser);
		}

		if (!MessageDialog.openQuestion(
				HandlerUtil.getActiveShell(event),
				"Delete Users",
				"Are you sure that you want to delete these users?\n"
						+ builder.substring(0, builder.length() - 2)))
			return null;

		userAdminWrapper.beginTransactionIfNeeded();
		UserAdmin userAdmin = userAdminWrapper.getUserAdmin();
		IWorkbenchPage iwp = HandlerUtil.getActiveWorkbenchWindow(event)
				.getActivePage();

		for (User user : users) {
			String userName = user.getName();
			// TODO find a way to close the editor cleanly if opened. Cannot be
			// done through the UserAdminListeners, it causes a
			// java.util.ConcurrentModificationException because disposing the
			// editor unregisters and disposes the listener
			IEditorPart part = iwp.findEditor(new UserEditorInput(userName));
			if (part != null)
				iwp.closeEditor(part, false);
			userAdmin.removeRole(userName);
		}
		userAdminWrapper.commitOrNotifyTransactionStateChange();

		for (User user : users) {
			userAdminWrapper.notifyListeners(new UserAdminEvent(null,
					UserAdminEvent.ROLE_REMOVED, user));
		}
		return null;
	}

	/* DEPENDENCY INJECTION */
	public void setUserAdminWrapper(UserAdminWrapper userAdminWrapper) {
		this.userAdminWrapper = userAdminWrapper;
	}
}