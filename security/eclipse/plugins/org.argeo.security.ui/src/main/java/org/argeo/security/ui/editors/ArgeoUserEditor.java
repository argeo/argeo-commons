package org.argeo.security.ui.editors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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
	private final static Log log = LogFactory.getLog(ArgeoUserEditor.class);

	public final static String ID = "org.argeo.security.ui.argeoUserEditor";

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
			user = securityService.getSecurityDao().getUser(username);
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

		if (securityService.getSecurityDao().userExists(user.getUsername()))
			securityService.updateUser(user);
		else {
			try {
				// FIXME: make it cleaner
				((SimpleArgeoUser)user).setPassword(user.getUsername());
				securityService.newUser(user);
				setPartName(user.getUsername());
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
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
