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
package org.argeo.security.ui.admin.commands;

import org.argeo.security.ui.admin.SecurityAdminPlugin;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.ui.handlers.HandlerUtil;
import org.osgi.service.useradmin.UserAdmin;

/** Create a new group. */
public class NewGroup extends AbstractHandler {
	public final static String ID = SecurityAdminPlugin.PLUGIN_ID + ".newGroup";

	private UserAdmin userAdmin;

	public Object execute(ExecutionEvent event) throws ExecutionException {

		MessageDialog
				.openError(HandlerUtil.getActiveShell(event),
						"Unimplemented method",
						"Group creation is not yet implemented");
		return null;
	}

	/* DEPENDENCY INJECTION */
	public void setUserAdmin(UserAdmin userAdmin) {
		this.userAdmin = userAdmin;
	}
}

// JcrRolesView rolesView = (JcrRolesView) HandlerUtil
// .getActiveWorkbenchWindow(event).getActivePage()
// .findView(JcrRolesView.ID);
// String role = rolesView.getNewRole();
// if (role.trim().equals(""))
// return null;
// if (role.equals(rolesView.getAddNewRoleText()))
// return null;
// role = role.trim().toUpperCase();
// if (!role.startsWith(rolePrefix))
// role = rolePrefix + role;
// if (userAdminService.listEditableRoles().contains(role))
// throw new ArgeoException("Role " + role + " already exists");
// userAdminService.newRole(role);
// rolesView.refresh();
//
// // refresh editors
// IEditorReference[] refs = HandlerUtil.getActiveWorkbenchWindow(event)
// .getActivePage()
// .findEditors(null, JcrArgeoUserEditor.ID, IWorkbenchPage.MATCH_ID);
// for (IEditorReference ref : refs) {
// JcrArgeoUserEditor userEditor = (JcrArgeoUserEditor) ref.getEditor(false);
// if (userEditor != null) {
// userEditor.refresh();
// }
// }
// return null;
// }
//
// public void setUserAdminService(UserAdminService userAdminService) {
// this.userAdminService = userAdminService;
// }
//
// }
