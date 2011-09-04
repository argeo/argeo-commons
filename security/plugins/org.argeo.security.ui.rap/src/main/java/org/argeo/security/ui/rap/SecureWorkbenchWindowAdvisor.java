package org.argeo.security.ui.rap;

import org.eclipse.swt.graphics.Point;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.actions.ActionFactory.IWorkbenchAction;
import org.eclipse.ui.application.ActionBarAdvisor;
import org.eclipse.ui.application.IActionBarConfigurer;
import org.eclipse.ui.application.IWorkbenchWindowConfigurer;
import org.eclipse.ui.application.WorkbenchWindowAdvisor;

public class SecureWorkbenchWindowAdvisor extends WorkbenchWindowAdvisor {

	public SecureWorkbenchWindowAdvisor(IWorkbenchWindowConfigurer configurer) {
		super(configurer);
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
		configurer.setShowStatusLine(false);

		configurer.setShowPerspectiveBar(true);
		configurer.setTitle("Argeo Secure UI"); //$NON-NLS-1$
	}

	@Override
	public void postWindowOpen() {
		String defaultPerspective = getWindowConfigurer()
				.getWorkbenchConfigurer().getWorkbench()
				.getPerspectiveRegistry().getDefaultPerspective();
		if (defaultPerspective == null) {
			IWorkbenchWindow window = getWindowConfigurer().getWindow();
			if (window == null)
				return;

			IWorkbenchAction openPerspectiveDialogAction = ActionFactory.OPEN_PERSPECTIVE_DIALOG
					.create(window);
			openPerspectiveDialogAction.run();
		}
	}

}
