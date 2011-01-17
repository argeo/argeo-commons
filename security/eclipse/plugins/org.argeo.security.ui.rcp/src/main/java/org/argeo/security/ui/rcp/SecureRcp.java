package org.argeo.security.ui.rcp;

import org.eclipse.equinox.app.IApplication;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.application.WorkbenchAdvisor;

public class SecureRcp extends AbstractSecureApplication {

	@Override
	protected WorkbenchAdvisor createWorkbenchAdvisor() {
		return new SecureWorkbenchAdvisor();
	}

	protected Integer processReturnCode(Integer returnCode) {
		if (returnCode == PlatformUI.RETURN_RESTART)
			return IApplication.EXIT_RESTART;
		else
			return IApplication.EXIT_OK;
	}

}
