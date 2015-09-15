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

import javax.transaction.UserTransaction;

import org.argeo.security.ui.admin.SecurityAdminPlugin;
import org.argeo.security.ui.admin.internal.UiAdminUtils;
import org.argeo.security.ui.admin.internal.UserAdminConstants;
import org.argeo.security.ui.admin.views.GroupsView;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;
import org.osgi.service.useradmin.Group;
import org.osgi.service.useradmin.UserAdmin;

/** Deletes the selected groups */
public class DeleteGroups extends AbstractHandler {
	public final static String ID = SecurityAdminPlugin.PLUGIN_ID
			+ ".deleteGroups";

	/* DEPENDENCY INJECTION */
	private UserAdmin userAdmin;
	private UserTransaction userTransaction;

	@SuppressWarnings("unchecked")
	public Object execute(ExecutionEvent event) throws ExecutionException {
		ISelection selection = HandlerUtil.getCurrentSelection(event);
		if (selection.isEmpty())
			return null;

		Map<String, String> toDelete = new TreeMap<String, String>();
		// List<String> names = new ArrayList<String>();
		Iterator<Group> it = ((IStructuredSelection) selection).iterator();
		while (it.hasNext()) {
			Group currGroup = it.next();
			String groupName = UiAdminUtils.getProperty(currGroup,
					UserAdminConstants.KEY_CN);
			// TODO add checks
			toDelete.put(groupName, currGroup.getName());
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

		UiAdminUtils.beginTransactionIfNeeded(userTransaction);
		for (String name : toDelete.values())
			userAdmin.removeRole(name);

		// TODO rather notify the update listener
		forceRefresh(event);
		return null;
	}

	private void forceRefresh(ExecutionEvent event) {
		IWorkbenchWindow iww = HandlerUtil.getActiveWorkbenchWindow(event);
		if (iww == null)
			return;
		IWorkbenchPage activePage = iww.getActivePage();
		IWorkbenchPart part = activePage.getActivePart();
		if (part instanceof GroupsView)
			((GroupsView) part).refresh();
	}

	/* DEPENDENCY INJECTION */
	public void setUserAdmin(UserAdmin userAdmin) {
		this.userAdmin = userAdmin;
	}

	public void setUserTransaction(UserTransaction userTransaction) {
		this.userTransaction = userTransaction;
	}
}