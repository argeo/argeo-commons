package org.argeo.cms.e4.handlers;

import java.security.AccessController;

import javax.security.auth.Subject;

import org.argeo.cms.auth.CurrentUser;
import org.argeo.cms.swt.CmsException;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.workbench.IWorkbench;

public class CloseWorkbench {
	@Execute
	public void execute(IWorkbench workbench) {
		logout();
		workbench.close();
	}

	protected void logout() {
		Subject subject = Subject.getSubject(AccessController.getContext());
		try {
			CurrentUser.logoutCmsSession(subject);
		} catch (Exception e) {
			throw new CmsException("Cannot log out", e);
		}
	}

}
