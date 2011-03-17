package org.argeo.security.ui.admin.editors;

import org.argeo.ArgeoException;
import org.argeo.security.ArgeoUser;
import org.argeo.security.SimpleArgeoUser;
import org.argeo.security.UserAdminService;
import org.argeo.security.nature.SimpleUserNature;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.editor.FormEditor;

/** Editor for an Argeo user. */
public class ArgeoUserEditor extends FormEditor {
	public final static String ID = "org.argeo.security.ui.admin.adminArgeoUserEditor";

	private ArgeoUser user;
	private UserAdminService userAdminService;

	public void init(IEditorSite site, IEditorInput input)
			throws PartInitException {
		super.init(site, input);
		String username = ((ArgeoUserEditorInput) getEditorInput())
				.getUsername();
		if (username == null) {// new
			user = new SimpleArgeoUser();
			user.getUserNatures().put(SimpleUserNature.TYPE,
					new SimpleUserNature());
		} else
			user = userAdminService.getUser(username);
		this.setPartProperty("name", username != null ? username : "<new user>");
		setPartName(username != null ? username : "<new user>");
	}

	protected void addPages() {
		try {
			addPage(new DefaultUserMainPage(this, userAdminService, user));

		} catch (PartInitException e) {
			throw new ArgeoException("Not able to add page ", e);
		}
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
		// list pages
		// TODO: make it more generic
		findPage(DefaultUserMainPage.ID).doSave(monitor);

		if (userAdminService.userExists(user.getUsername()))
			userAdminService.updateUser(user);
		else {
			userAdminService.newUser(user);
			setPartName(user.getUsername());
		}
		firePropertyChange(PROP_DIRTY);
	}

	@Override
	public void doSaveAs() {
	}

	@Override
	public boolean isSaveAsAllowed() {
		return false;
	}

	public void setUserAdminService(UserAdminService userAdminService) {
		this.userAdminService = userAdminService;
	}
}
