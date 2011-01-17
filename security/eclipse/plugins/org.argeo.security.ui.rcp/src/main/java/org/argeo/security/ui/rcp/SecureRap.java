package org.argeo.security.ui.rcp;

import java.security.PrivilegedAction;

import javax.security.auth.Subject;

import org.argeo.security.equinox.CurrentUser;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.application.IWorkbenchWindowConfigurer;
import org.eclipse.ui.application.WorkbenchWindowAdvisor;

public class SecureRap implements IApplication {
	public Object start(IApplicationContext context) throws Exception {
		String username = CurrentUser.getUsername();
		Integer result = null;
		Display display = PlatformUI.createDisplay();
		try {
			result = (Integer) Subject.doAs(CurrentUser.getSubject(),
					getRunAction(display));
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			display.dispose();
		}
		return result;
	}

	@SuppressWarnings("rawtypes")
	private PrivilegedAction getRunAction(final Display display) {
		return new PrivilegedAction() {

			public Object run() {
				int result = PlatformUI.createAndRunWorkbench(display,
						new ApplicationWorkbenchAdvisor());
				return new Integer(result);
			}
		};
	}

	public void stop() {
		final IWorkbench workbench;
		try {
			workbench = PlatformUI.getWorkbench();
		} catch (Exception e) {
			return;
		}
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

	class ApplicationWorkbenchAdvisor extends SecureWorkbenchAdvisor {
		public WorkbenchWindowAdvisor createWorkbenchWindowAdvisor(
				IWorkbenchWindowConfigurer configurer) {
			return new RapSecureWorkbenchWindowAdvisor(configurer);
		}
	}

}
