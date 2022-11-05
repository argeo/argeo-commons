package org.argeo.cms.e4.handlers;

import javax.security.auth.Subject;

import org.argeo.cms.auth.CurrentUser;
import org.argeo.cms.util.CurrentSubject;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.workbench.IWorkbench;

public class CloseWorkbench {
	@Execute
	public void execute(IWorkbench workbench) {
		logout();
		workbench.close();
	}

	protected void logout() {
		Subject subject = CurrentSubject.current();
		try {
			CurrentUser.logoutCmsSession(subject);
		} catch (Exception e) {
			throw new IllegalStateException("Cannot log out", e);
		}
	}

}
