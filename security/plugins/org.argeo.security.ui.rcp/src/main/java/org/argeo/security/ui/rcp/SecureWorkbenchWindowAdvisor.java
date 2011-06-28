package org.argeo.security.ui.rcp;

import org.eclipse.swt.graphics.Point;
import org.eclipse.ui.application.ActionBarAdvisor;
import org.eclipse.ui.application.IActionBarConfigurer;
import org.eclipse.ui.application.IWorkbenchWindowConfigurer;
import org.eclipse.ui.application.WorkbenchWindowAdvisor;

public class SecureWorkbenchWindowAdvisor extends WorkbenchWindowAdvisor {
	private final String username;

	public SecureWorkbenchWindowAdvisor(IWorkbenchWindowConfigurer configurer,
			String username) {
		super(configurer);
		this.username = username;
	}

	public ActionBarAdvisor createActionBarAdvisor(
			IActionBarConfigurer configurer) {
		return new SecureActionBarAdvisor(configurer, true);
	}

	public void preWindowOpen() {
		IWorkbenchWindowConfigurer configurer = getWindowConfigurer();
		configurer.setInitialSize(new Point(1200, 900));
		configurer.setShowCoolBar(true);
		configurer.setShowMenuBar(true);
		configurer.setShowStatusLine(true);
		configurer.setShowProgressIndicator(true);

		configurer.setShowPerspectiveBar(true);
		configurer.setTitle("Argeo UI - " + username); //$NON-NLS-1$

	}
}
