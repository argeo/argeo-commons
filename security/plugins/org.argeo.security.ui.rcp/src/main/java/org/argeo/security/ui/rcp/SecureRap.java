package org.argeo.security.ui.rcp;

import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.application.IWorkbenchWindowConfigurer;
import org.eclipse.ui.application.WorkbenchAdvisor;
import org.eclipse.ui.application.WorkbenchWindowAdvisor;

/** Generic secure application for RAP. */
public class SecureRap extends AbstractSecureApplication {

	@Override
	protected WorkbenchAdvisor createWorkbenchAdvisor() {
		return new SecureWorkbenchAdvisor() {
			public WorkbenchWindowAdvisor createWorkbenchWindowAdvisor(
					IWorkbenchWindowConfigurer configurer) {
				return new RapSecureWorkbenchWindowAdvisor(configurer);
			}

		};
	}

	public void stop() {
		final IWorkbench workbench = PlatformUI.getWorkbench();
		if (workbench == null)
			return;
		final Display display = workbench.getDisplay();
		display.syncExec(new Runnable() {

			public void run() {
				if (!display.isDisposed())
					workbench.close();
			}
		});
	}

}
