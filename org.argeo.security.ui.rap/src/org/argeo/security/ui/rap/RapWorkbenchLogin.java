package org.argeo.security.ui.rap;

import org.eclipse.rap.rwt.RWT;
import org.eclipse.rap.rwt.client.service.JavaScriptExecutor;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;

public class RapWorkbenchLogin extends WorkbenchLogin {
	// private final static Log log =
	// LogFactory.getLog(RapWorkbenchLogin.class);

	@Override
	protected int createAndRunWorkbench(Display display, String username) {
		RapWorkbenchAdvisor workbenchAdvisor = createRapWorkbenchAdvisor(username);
		return PlatformUI.createAndRunWorkbench(display, workbenchAdvisor);
	}

	/** Override to provide an application specific workbench advisor */
	protected RapWorkbenchAdvisor createRapWorkbenchAdvisor(String username) {
		return new RapWorkbenchAdvisor(username);
	}

	@Override
	public int createUI() {
		JavaScriptExecutor jsExecutor = RWT.getClient().getService(
				JavaScriptExecutor.class);
		int returnCode;
		try {
			returnCode = super.createUI();
		} finally {
			// always reload
			jsExecutor.execute("location.reload()");
		}
		return returnCode;
	}

}
