package org.argeo.security.ui.rap;

import javax.servlet.http.HttpServletRequest;

import org.argeo.security.ui.login.WorkbenchLogin;
import org.eclipse.rap.rwt.RWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;

public class RapLoginEntryPoint extends WorkbenchLogin {

	@Override
	protected int createAndRunWorkbench(Display display, String username) {
		RapWorkbenchAdvisor workbenchAdvisor = createRapWorkbenchAdvisor(username);
		return PlatformUI.createAndRunWorkbench(display, workbenchAdvisor);
	}

	/** Override to provide an application specific workbench advisor */
	protected RapWorkbenchAdvisor createRapWorkbenchAdvisor(String username) {
		return new RapWorkbenchAdvisor(username);
	}

	protected HttpServletRequest getRequest() {
		return RWT.getRequest();
	}
}
