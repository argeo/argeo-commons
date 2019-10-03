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
package org.argeo.cms.e4.users.handlers;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.argeo.cms.e4.users.GroupsView;
import org.argeo.cms.e4.users.UserAdminWrapper;
import org.argeo.cms.util.UserAdminUtils;
import org.eclipse.e4.core.di.annotations.CanExecute;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.services.IServiceConstants;
import org.eclipse.e4.ui.workbench.modeling.ESelectionService;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.osgi.service.useradmin.Group;
import org.osgi.service.useradmin.UserAdmin;
import org.osgi.service.useradmin.UserAdminEvent;

/** Delete the selected groups */
public class DeleteGroups {
	// public final static String ID = WorkbenchUiPlugin.PLUGIN_ID +
	// ".deleteGroups";

	/* DEPENDENCY INJECTION */
	@Inject
	private UserAdminWrapper userAdminWrapper;

	@Inject
	ESelectionService selectionService;

	@SuppressWarnings("unchecked")
	@Execute
	public void execute(@Named(IServiceConstants.ACTIVE_PART) MPart part, ESelectionService selectionService) {
		// ISelection selection = null;// HandlerUtil.getCurrentSelection(event);
		// if (selection.isEmpty())
		// return null;
		//
		// List<Group> groups = new ArrayList<Group>();
		// Iterator<Group> it = ((IStructuredSelection) selection).iterator();

		List<Group> selection = (List<Group>) selectionService.getSelection();
		if (selection == null)
			return;

		StringBuilder builder = new StringBuilder();
		for (Group group : selection) {
			Group currGroup = group;
			String groupName = UserAdminUtils.getUserLocalId(currGroup.getName());
			// TODO add checks
			builder.append(groupName).append("; ");
			// groups.add(currGroup);
		}

		if (!MessageDialog.openQuestion(Display.getCurrent().getActiveShell(), "Delete Groups", "Are you sure that you "
				+ "want to delete these groups?\n" + builder.substring(0, builder.length() - 2)))
			return;

		userAdminWrapper.beginTransactionIfNeeded();
		UserAdmin userAdmin = userAdminWrapper.getUserAdmin();
		// IWorkbenchPage iwp =
		// HandlerUtil.getActiveWorkbenchWindow(event).getActivePage();
		for (Group group : selection) {
			String groupName = group.getName();
			// TODO find a way to close the editor cleanly if opened. Cannot be
			// done through the UserAdminListeners, it causes a
			// java.util.ConcurrentModificationException because disposing the
			// editor unregisters and disposes the listener
			// IEditorPart part = iwp.findEditor(new UserEditorInput(groupName));
			// if (part != null)
			// iwp.closeEditor(part, false);
			userAdmin.removeRole(groupName);
		}
		userAdminWrapper.commitOrNotifyTransactionStateChange();

		// Update the view
		for (Group group : selection) {
			userAdminWrapper.notifyListeners(new UserAdminEvent(null, UserAdminEvent.ROLE_REMOVED, group));
		}

		// return null;
	}

	@CanExecute
	public boolean canExecute(@Named(IServiceConstants.ACTIVE_PART) MPart part, ESelectionService selectionService) {
		return part.getObject() instanceof GroupsView && selectionService.getSelection() != null;
	}

	/* DEPENDENCY INJECTION */
	// public void setUserAdminWrapper(UserAdminWrapper userAdminWrapper) {
	// this.userAdminWrapper = userAdminWrapper;
	// }
}
