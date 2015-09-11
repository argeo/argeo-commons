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

import javax.jcr.Repository;

import org.argeo.security.UserAdminService;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;

/**
 * Refreshes the main user list, removing nodes which are not referenced by user
 * admin service.
 */
public class RefreshUsersList extends AbstractHandler {
	private UserAdminService userAdminService;
	private Repository repository;

	public Object execute(ExecutionEvent event) throws ExecutionException {
		// Set<String> users = userAdminService.listUsers();
		// Session session = null;
		// try {
		// session = repository.login();
		// Query query = session
		// .getWorkspace()
		// .getQueryManager()
		// .createQuery(
		// "select * from [" + ArgeoTypes.ARGEO_USER_HOME
		// + "]", Query.JCR_SQL2);
		// NodeIterator nit = query.execute().getNodes();
		// while (nit.hasNext()) {
		// Node node = nit.nextNode();
		// String username = node.getProperty(ArgeoNames.ARGEO_USER_ID)
		// .getString();
		// if (!users.contains(username))
		// node.remove();
		// }
		// session.save();
		// } catch (RepositoryException e) {
		// JcrUtils.discardQuietly(session);
		// throw new ArgeoException("Cannot list users", e);
		// } finally {
		// JcrUtils.logoutQuietly(session);
		// }
		// userAdminService.synchronize();
		//
		// // FIXME try to refresh views that extend the JcrUsersView and have
		// another
		// // ID
		// IWorkbenchPart part = HandlerUtil.getActiveWorkbenchWindow(event)
		// .getActivePage().getActivePart();
		// if (part instanceof JcrUsersView)
		// ((JcrUsersView) part).refresh();
		//
		// // Try to refresh JcrUsersView if opened
		// JcrUsersView view = (JcrUsersView) HandlerUtil
		// .getActiveWorkbenchWindow(event).getActivePage()
		// .findView(JcrUsersView.ID);
		// if (view != null)
		// view.refresh();

		return null;
	}

	public void setUserAdminService(UserAdminService userAdminService) {
		this.userAdminService = userAdminService;
	}

	public void setRepository(Repository repository) {
		this.repository = repository;
	}

}