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

import org.argeo.ArgeoException;
import org.argeo.security.ui.admin.SecurityAdminImages;
import org.argeo.security.ui.admin.SecurityAdminPlugin;
import org.argeo.security.ui.admin.UserAdminConstants;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.editor.FormEditor;
import org.osgi.service.useradmin.Role;
import org.osgi.service.useradmin.User;
import org.osgi.service.useradmin.UserAdmin;

/** Editor for an Argeo user. */
public class UserEditor extends FormEditor implements UserAdminConstants {
	private static final long serialVersionUID = 8357851520380820241L;

	public final static String ID = SecurityAdminPlugin.PLUGIN_ID
			+ ".userEditor";

	/* DEPENDENCY INJECTION */
	private UserAdmin userAdmin;

	// Context
	private User user;
	private String username;

	public void init(IEditorSite site, IEditorInput input)
			throws PartInitException {
		super.init(site, input);
		username = ((UserEditorInput) getEditorInput()).getUsername();
		user = (User) userAdmin.getRole(username);

		String commonName = getProperty(KEY_CN);
		// this.setPartProperty("name", commonName != null ? commonName
		// : "username");

		// if (user.getType() == Role.GROUP) {
		// this.setPartProperty("icon", "icons/users.gif");
		// firePartPropertyChanged("icon", "icons/user.gif", "icons/users.gif");
		// }
		setPartName(commonName != null ? commonName : "username");
	}

	protected void addPages() {
		try {
			
			if (user.getType() == Role.GROUP)
				addPage(new GroupMainPage(this, userAdmin));
			else
				addPage(new UserMainPage(this));
			
			
			
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

	/** The property is directly updated!!! */
	@SuppressWarnings("unchecked")
	protected void setProperty(String key, String value) {
		user.getProperties().put(key, value);
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
		commitPages(true);
		firePropertyChange(PROP_DIRTY);
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
}