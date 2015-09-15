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
import java.util.List;

import javax.transaction.UserTransaction;

import org.argeo.ArgeoException;
import org.argeo.security.ui.admin.SecurityAdminPlugin;
import org.argeo.security.ui.admin.internal.UiAdminUtils;
import org.argeo.security.ui.admin.internal.UserAdminConstants;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.editor.FormEditor;
import org.osgi.service.useradmin.Authorization;
import org.osgi.service.useradmin.Role;
import org.osgi.service.useradmin.User;
import org.osgi.service.useradmin.UserAdmin;

/** Editor for a user, might be a user or a group. */
public class UserEditor extends FormEditor implements UserAdminConstants {
	private static final long serialVersionUID = 8357851520380820241L;

	public final static String ID = SecurityAdminPlugin.PLUGIN_ID
			+ ".userEditor";

	/* DEPENDENCY INJECTION */
	private UserAdmin userAdmin;
	private UserTransaction userTransaction;

	// Context
	private User user;
	private String username;

	public void init(IEditorSite site, IEditorInput input)
			throws PartInitException {
		super.init(site, input);
		username = ((UserEditorInput) getEditorInput()).getUsername();
		user = (User) userAdmin.getRole(username);

		String commonName = getProperty(KEY_CN);

		setPartName(commonName != null ? commonName : "username");

		// TODO: following has been disabled because it causes NPE after a
		// login/logout on RAP
		// Image titleIcon = user.getType() == Role.GROUP ?
		// SecurityAdminImages.ICON_GROUP
		// : SecurityAdminImages.ICON_USER;
		// setTitleImage(titleIcon);
	}

	/**
	 * returns the list of all authorisation for the given user or of the
	 * current displayed user if parameter is null
	 */
	protected List<User> getFlatGroups(User aUser) {
		Authorization currAuth;
		if (aUser == null)
			currAuth = userAdmin.getAuthorization(this.user);
		else
			currAuth = userAdmin.getAuthorization(aUser);

		String[] roles = currAuth.getRoles();

		List<User> groups = new ArrayList<User>();
		for (String roleStr : roles) {
			User currRole = (User) userAdmin.getRole(roleStr);
			if (!groups.contains(currRole))
				groups.add(currRole);
		}
		return groups;
	}

	/** Exposes the user (or group) that is displayed by the current editor */
	protected User getDisplayedUser() {
		return user;
	}

	void updateEditorTitle(String title) {
		setPartName(title);
	}

	protected void addPages() {
		try {
			if (user.getType() == Role.GROUP)
				addPage(new GroupMainPage(this, userAdmin));
			else
				addPage(new UserMainPage(this, userAdmin));
		} catch (Exception e) {
			throw new ArgeoException("Cannot add pages", e);
		}
	}

	protected String getProperty(String key) {
		Object obj = user.getProperties().get(key);
		if (obj != null)
			return (String) obj;
		else
			return "";
	}

	protected void beginTransactionIfNeeded() {
		UiAdminUtils.beginTransactionIfNeeded(userTransaction);
	}

	/**
	 * Updates the property in the working copy. The transaction must be
	 * explicitly committed to persist the update.
	 */
	@SuppressWarnings("unchecked")
	protected void setProperty(String key, String value) {
		user.getProperties().put(key, value);
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
		UiAdminUtils.beginTransactionIfNeeded(userTransaction);
		commitPages(true);
		firePropertyChange(PROP_DIRTY);
		// FIXME transaction should be managed at a higher level
		// try {
		// userTransaction.commit();
		// } catch (Exception e) {
		// throw new ArgeoException("Could not save user editor", e);
		// }
	}

	@Override
	public void doSaveAs() {
	}

	@Override
	public boolean isSaveAsAllowed() {
		return false;
	}

	public void refresh() {

	}

	@Override
	public void dispose() {
		super.dispose();
	}

	/* DEPENDENCY INJECTION */
	public void setUserAdmin(UserAdmin userAdmin) {
		this.userAdmin = userAdmin;
	}

	public void setUserTransaction(UserTransaction userTransaction) {
		this.userTransaction = userTransaction;
	}

}