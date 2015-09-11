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

import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import org.argeo.security.ui.admin.SecurityAdminPlugin;
import org.argeo.security.ui.admin.internal.UiAdminUtils;
import org.argeo.security.ui.admin.internal.UserAdminConstants;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.handlers.HandlerUtil;
import org.osgi.service.useradmin.Group;

/** Deletes the selected groups */
public class DeleteGroups extends AbstractHandler {
	public final static String ID = SecurityAdminPlugin.PLUGIN_ID + ".deleteGroups";

	
	@SuppressWarnings("unchecked")
	public Object execute(ExecutionEvent event) throws ExecutionException {
		ISelection selection = HandlerUtil.getCurrentSelection(event);
		if (selection.isEmpty())
			return null;

		Map<String, Group> toDelete = new TreeMap<String, Group>();
		Iterator<Group> it = ((IStructuredSelection) selection).iterator();
		groups: while (it.hasNext()) {
			Group currGroup = it.next();
			String groupName = UiAdminUtils.getProperty(currGroup,
					UserAdminConstants.KEY_CN);

			// TODO add checks
			// if (userName.equals(profileNode.getSession().getUserID())) {
			// log.warn("Cannot delete its own user: " + userName);
			// continue groups;
			// }
			toDelete.put(groupName, currGroup);
		}

		if (!MessageDialog
				.openQuestion(
						HandlerUtil.getActiveShell(event),
						"Delete Groups",
						"Are you sure that you want to delete groups "
								+ toDelete.keySet()
								+ "?\n"
								+ "This might lead to inconsistencies in the application."))
			return null;

		for (String groupName : toDelete.keySet()) {
		}
		MessageDialog.openInformation(HandlerUtil.getActiveShell(event),
				"Unimplemented method",
				"The effective deletion is not yet implemented");
		// TODO refresh?
		// JcrUsersView view = (JcrUsersView) HandlerUtil
		// .getActiveWorkbenchWindow(event).getActivePage()
		// .findView(JcrUsersView.ID);
		// view.refresh();
		return null;
	}

	// public void setUserAdmin(UserAdmin userAdmin) {
	// this.userAdmin = userAdmin;
	// }
}