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

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.argeo.ArgeoException;
import org.argeo.jcr.ArgeoNames;
import org.argeo.jcr.JcrUtils;
import org.argeo.security.UserAdminService;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.handlers.HandlerUtil;

/** Deletes the selected user nodes */
public class DeleteUser extends AbstractHandler {
	private final static Log log = LogFactory.getLog(DeleteUser.class);

	private UserAdminService userAdminService;

	@SuppressWarnings("unchecked")
	public Object execute(ExecutionEvent event) throws ExecutionException {
		ISelection selection = HandlerUtil.getCurrentSelection(event);
		if (selection.isEmpty())
			return null;

		Map<String, Node> toDelete = new TreeMap<String, Node>();
		Iterator<Node> it = ((IStructuredSelection) selection).iterator();
		nodes: while (it.hasNext()) {
			Node profileNode = it.next();
			try {
				String userName = profileNode.getProperty(
						ArgeoNames.ARGEO_USER_ID).getString();
				if (userName.equals(profileNode.getSession().getUserID())) {
					log.warn("Cannot delete its own user: " + userName);
					continue nodes;
				}
				toDelete.put(userName, profileNode);
			} catch (RepositoryException e) {
				log.warn("Cannot interpred user " + profileNode);
			}
		}

		if (!MessageDialog
				.openQuestion(
						HandlerUtil.getActiveShell(event),
						"Delete User",
						"Are you sure that you want to delete users "
								+ toDelete.keySet()
								+ "?\n"
								+ "This may lead to inconsistencies in the application."))
			return null;

		for (String username : toDelete.keySet()) {
			Session session = null;
			try {
				Node profileNode = toDelete.get(username);
				userAdminService.deleteUser(username);
				profileNode.getParent().remove();
				session = profileNode.getSession();
				session.save();
			} catch (RepositoryException e) {
				JcrUtils.discardQuietly(session);
				throw new ArgeoException("Cannot list users", e);
			}
		}

		userAdminService.synchronize();
		// JcrUsersView view = (JcrUsersView) HandlerUtil
		// .getActiveWorkbenchWindow(event).getActivePage()
		// .findView(JcrUsersView.ID);
		// view.refresh();
		return null;
	}

	public void setUserAdminService(UserAdminService userAdminService) {
		this.userAdminService = userAdminService;
	}
}