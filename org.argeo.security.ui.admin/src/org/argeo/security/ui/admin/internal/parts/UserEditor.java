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
package org.argeo.security.ui.admin.internal.parts;

import java.util.ArrayList;
import java.util.List;

import org.argeo.ArgeoException;
import org.argeo.security.ui.admin.SecurityAdminPlugin;
import org.argeo.security.ui.admin.internal.UiAdminUtils;
import org.argeo.security.ui.admin.internal.UserAdminConstants;
import org.argeo.security.ui.admin.internal.UserAdminWrapper;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.AbstractFormPart;
import org.eclipse.ui.forms.editor.FormEditor;
import org.osgi.service.useradmin.Authorization;
import org.osgi.service.useradmin.Role;
import org.osgi.service.useradmin.User;
import org.osgi.service.useradmin.UserAdmin;
import org.osgi.service.useradmin.UserAdminEvent;
import org.osgi.service.useradmin.UserAdminListener;

/** Editor for a user, might be a user or a group. */
public class UserEditor extends FormEditor implements UserAdminConstants {
	private static final long serialVersionUID = 8357851520380820241L;

	public final static String USER_EDITOR_ID = SecurityAdminPlugin.PLUGIN_ID
			+ ".userEditor";
	public final static String GROUP_EDITOR_ID = SecurityAdminPlugin.PLUGIN_ID
			+ ".groupEditor";

	/* DEPENDENCY INJECTION */
	private UserAdminWrapper userAdminWrapper;
	private UserAdmin userAdmin;

	// Context
	private User user;
	private String username;

	private NameChangeListener listener;

	public void init(IEditorSite site, IEditorInput input)
			throws PartInitException {
		super.init(site, input);
		username = ((UserEditorInput) getEditorInput()).getUsername();
		user = (User) userAdmin.getRole(username);

		listener = new NameChangeListener(user);
		userAdminWrapper.addListener(listener);
		updateEditorTitle(null);
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
		if (title == null) {
			String commonName = UiAdminUtils.getProperty(user, KEY_CN);
			title = "".equals(commonName) ? user.getName() : commonName;
		}
		setPartName(title);
	}

	protected void addPages() {
		try {
			if (user.getType() == Role.GROUP)
				addPage(new GroupMainPage(this, userAdminWrapper));
			else
				addPage(new UserMainPage(this, userAdminWrapper));
		} catch (Exception e) {
			throw new ArgeoException("Cannot add pages", e);
		}
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
		userAdminWrapper.beginTransactionIfNeeded();
		commitPages(true);
		firePropertyChange(PROP_DIRTY);
		userAdminWrapper.notifyListeners(new UserAdminEvent(null,
				UserAdminEvent.ROLE_REMOVED, user));
	}

	@Override
	public void doSaveAs() {
	}

	@Override
	public boolean isSaveAsAllowed() {
		return false;
	}

	@Override
	public void dispose() {
		userAdminWrapper.removeListener(listener);
		super.dispose();
	}

	// CONTROLERS FOR THIS EDITOR AND ITS PAGES

	class NameChangeListener implements UserAdminListener {

		private final User user;

		public NameChangeListener(User user) {
			this.user = user;
		}

		@Override
		public void roleChanged(UserAdminEvent event) {
			Role changedRole = event.getRole();
			if (changedRole == null || changedRole.equals(user))
				updateEditorTitle(null);
		}
	}

	class MainInfoListener implements UserAdminListener {
		private final AbstractFormPart part;

		public MainInfoListener(AbstractFormPart part) {
			this.part = part;
		}

		@Override
		public void roleChanged(UserAdminEvent event) {
			// Rollback
			if (event.getRole() == null)
				part.markStale();
		}
	}

	class GroupChangeListener implements UserAdminListener {
		private final AbstractFormPart part;

		public GroupChangeListener(AbstractFormPart part) {
			this.part = part;
		}

		@Override
		public void roleChanged(UserAdminEvent event) {
			// always mark as stale
			part.markStale();
		}
	}

	/** Registers a listener that will notify this part */
	class FormPartML implements ModifyListener {
		private static final long serialVersionUID = 6299808129505381333L;
		private AbstractFormPart formPart;

		public FormPartML(AbstractFormPart generalPart) {
			this.formPart = generalPart;
		}

		public void modifyText(ModifyEvent e) {
			// Discard event when the control does not have the focus, typically
			// to avoid all editors being marked as dirty during a Rollback
			if (((Control) e.widget).isFocusControl())
				formPart.markDirty();
		}
	}

	/* DEPENDENCY INJECTION */
	public void setUserAdminWrapper(UserAdminWrapper userAdminWrapper) {
		this.userAdminWrapper = userAdminWrapper;
		this.userAdmin = userAdminWrapper.getUserAdmin();
	}
}