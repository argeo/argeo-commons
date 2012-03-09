/*
 * Copyright (C) 2007-2012 Mathieu Baudier
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
package org.argeo.security.ui.admin.commands;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.argeo.security.UserAdminService;
import org.argeo.security.ui.admin.views.RolesView;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.handlers.HandlerUtil;

/** Deletes the selected roles */
public class DeleteRole extends AbstractHandler {
	private UserAdminService userAdminService;

	@SuppressWarnings("unchecked")
	public Object execute(ExecutionEvent event) throws ExecutionException {
		ISelection selection = HandlerUtil.getCurrentSelection(event);
		if (selection.isEmpty())
			return null;

		List<String> toDelete = new ArrayList<String>();
		Iterator<String> it = ((IStructuredSelection) selection).iterator();
		while (it.hasNext()) {
			toDelete.add(it.next());
		}

		if (!MessageDialog
				.openQuestion(
						HandlerUtil.getActiveShell(event),
						"Delete Role",
						"Are you sure that you want to delete "
								+ toDelete
								+ "?\n"
								+ "This may lead to inconsistencies in the application."))
			return null;

		for (String role : toDelete) {
			userAdminService.deleteRole(role);
		}

		RolesView view = (RolesView) HandlerUtil
				.getActiveWorkbenchWindow(event).getActivePage()
				.findView(RolesView.ID);
		view.refresh();
		return null;
	}

	public void setUserAdminService(UserAdminService userAdminService) {
		this.userAdminService = userAdminService;
	}
}