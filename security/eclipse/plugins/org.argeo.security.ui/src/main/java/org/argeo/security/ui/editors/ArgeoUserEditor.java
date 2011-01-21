package org.argeo.security.ui.editors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.argeo.ArgeoException;
import org.argeo.security.ArgeoSecurityService;
import org.argeo.security.ArgeoUser;
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
		user = securityService.getSecurityDao().getUser(username);
		this.setPartProperty("name", username);
		setPartName(username);
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
		if (log.isDebugEnabled())
			log.debug("doSave called");
		// for (int i = 0; i < getPageCount(); i++) {
		// IEditorPart editor = getEditor(i);
		// if (editor != null)
		// editor.doSave(monitor);
		// }
		findPage(DefaultUserMainPage.ID).doSave(monitor);

		securityService.updateUser(user);
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
