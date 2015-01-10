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
package org.argeo.security.ui.admin.editors;

import java.util.ArrayList;

import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.argeo.ArgeoException;
import org.argeo.jcr.JcrUtils;
import org.argeo.jcr.UserJcrUtils;
import org.argeo.security.UserAdminService;
import org.argeo.security.jcr.JcrUserDetails;
import org.argeo.security.ui.admin.SecurityAdminPlugin;
import org.argeo.security.ui.admin.views.UsersView;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.editor.FormEditor;
import org.springframework.security.core.GrantedAuthority;

/** Editor for an Argeo user. */
public class ArgeoUserEditor extends FormEditor {
	private static final long serialVersionUID = 1933296330339252869L;

	public final static String ID = SecurityAdminPlugin.PLUGIN_ID
			+ ".adminArgeoUserEditor";

	/* DEPENDENCY INJECTION */
	private Session session;
	private UserAdminService userAdminService;

	// private Node userHome;
	private Node userProfile;
	private JcrUserDetails userDetails;

	public void init(IEditorSite site, IEditorInput input)
			throws PartInitException {
		super.init(site, input);
		String username = ((ArgeoUserEditorInput) getEditorInput())
				.getUsername();
		userProfile = UserJcrUtils.getUserProfile(session, username);

		if (userAdminService.userExists(username)) {
			try {
				userDetails = (JcrUserDetails) userAdminService
						.loadUserByUsername(username);
			} catch (Exception e) {
				throw new ArgeoException("Cannot retrieve userDetails for "
						+ username, e);
			}
		} else {
			try {
				userDetails = new JcrUserDetails(session, username, null,
						new ArrayList<GrantedAuthority>());
			} catch (RepositoryException e) {
				throw new ArgeoException("Cannot retrieve disabled JCR profile");
			}
		}

		this.setPartProperty("name", username != null ? username : "<new user>");
		setPartName(username != null ? username : "<new user>");
	}

	protected void addPages() {
		try {
			addPage(new DefaultUserMainPage(this, userProfile));
			addPage(new UserRolesPage(this, userDetails, userAdminService));
		} catch (Exception e) {
			throw new ArgeoException("Cannot add pages", e);
		}
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
		// list pages
		// TODO: make it more generic
		DefaultUserMainPage defaultUserMainPage = (DefaultUserMainPage) findPage(DefaultUserMainPage.ID);
		if (defaultUserMainPage.isDirty()) {
			defaultUserMainPage.doSave(monitor);
			String newPassword = defaultUserMainPage.getNewPassword();
			defaultUserMainPage.resetNewPassword();
			if (newPassword != null)
				userDetails = userDetails.cloneWithNewPassword(newPassword);
		}

		UserRolesPage userRolesPage = (UserRolesPage) findPage(UserRolesPage.ID);
		if (userRolesPage.isDirty()) {
			userRolesPage.doSave(monitor);
			userDetails = userDetails.cloneWithNewRoles(userRolesPage
					.getRoles());
		}

		userAdminService.updateUser(userDetails);

		// if (userAdminService.userExists(user.getUsername()))
		// userAdminService.updateUser(user);
		// else {
		// userAdminService.newUser(user);
		// setPartName(user.getUsername());
		// }
		firePropertyChange(PROP_DIRTY);

		userRolesPage.setUserDetails(userDetails);

		// FIXME rather use a refresh command. Fails when called by another
		// view.
		// refresh users view
		IWorkbench iw = SecurityAdminPlugin.getDefault().getWorkbench();
		UsersView usersView = (UsersView) iw.getActiveWorkbenchWindow()
				.getActivePage().findView(UsersView.ID);
		if (usersView != null)
			usersView.refresh();
	}

	@Override
	public void doSaveAs() {
	}

	@Override
	public boolean isSaveAsAllowed() {
		return false;
	}

	public void refresh() {
		UserRolesPage userRolesPage = (UserRolesPage) findPage(UserRolesPage.ID);
		userRolesPage.refresh();
	}

	@Override
	public void dispose() {
		JcrUtils.logoutQuietly(session);
		super.dispose();
	}

	/* DEPENDENCY INJECTION */
	public void setUserAdminService(UserAdminService userAdminService) {
		this.userAdminService = userAdminService;
	}

	public void setRepository(Repository repository) {
		try {
			session = repository.login();
		} catch (RepositoryException re) {
			throw new ArgeoException("Unable to initialise local session", re);
		}
	}
}
