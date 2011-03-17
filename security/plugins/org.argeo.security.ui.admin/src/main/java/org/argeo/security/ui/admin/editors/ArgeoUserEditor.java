package org.argeo.security.ui.admin.editors;

import org.argeo.ArgeoException;
import org.argeo.security.ArgeoSecurityService;
import org.argeo.security.ArgeoUser;
import org.argeo.security.SimpleArgeoUser;
import org.argeo.security.nature.SimpleUserNature;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.editor.FormEditor;

/** Editor for an Argeo user. */
public class ArgeoUserEditor extends FormEditor {
	public final static String ID = "org.argeo.security.ui.adminArgeoUserEditor";

	private ArgeoUser user;
	private ArgeoSecurityService securityService;

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
			user = securityService.getUser(username);
		this.setPartProperty("name", username != null ? username : "<new user>");
		setPartName(username != null ? username : "<new user>");
	}

	protected void addPages() {
		try {
			addPage(new DefaultUserMainPage(this, securityService, user));

		} catch (PartInitException e) {
			throw new ArgeoException("Not able to add page ", e);
		}
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
		// list pages
		// TODO: make it more generic
		findPage(DefaultUserMainPage.ID).doSave(monitor);

		if (securityService.userExists(user.getUsername()))
			securityService.updateUser(user);
		else {
			securityService.newUser(user);
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

	public void setSecurityService(ArgeoSecurityService securityService) {
		this.securityService = securityService;
	}
}
