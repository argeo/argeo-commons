package org.argeo.security.ui.rap;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.actions.ActionFactory.IWorkbenchAction;
import org.eclipse.ui.application.ActionBarAdvisor;
import org.eclipse.ui.application.IActionBarConfigurer;
import org.eclipse.ui.application.IWorkbenchWindowConfigurer;
import org.eclipse.ui.application.WorkbenchWindowAdvisor;

/** Eclipse RAP specific window advisor */
public class RapWindowAdvisor extends WorkbenchWindowAdvisor {

	private String username;

	public RapWindowAdvisor(IWorkbenchWindowConfigurer configurer,
			String username) {
		super(configurer);
		this.username = username;
	}

	@Override
	public ActionBarAdvisor createActionBarAdvisor(
			IActionBarConfigurer configurer) {
		return new RapActionBarAdvisor(configurer, username);
	}

	public void preWindowOpen() {
		IWorkbenchWindowConfigurer configurer = getWindowConfigurer();
		configurer.setShowCoolBar(true);
		configurer.setShowMenuBar(false);
		configurer.setShowStatusLine(false);
		configurer.setShowPerspectiveBar(true);
		configurer.setTitle("Argeo Secure UI"); //$NON-NLS-1$
		// Full screen, see
		// http://dev.eclipse.org/newslists/news.eclipse.technology.rap/msg02697.html
		configurer.setShellStyle(SWT.NONE);
		Rectangle bounds = Display.getCurrent().getBounds();
		configurer.setInitialSize(new Point(bounds.width, bounds.height));
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
