package org.argeo.security.ui.rcp;

import java.security.PrivilegedAction;

import javax.security.auth.Subject;

import org.argeo.security.equinox.CurrentUser;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;

public class SecureRcp implements IApplication {
	public Object start(IApplicationContext context) throws Exception {
		String username = CurrentUser.getUsername();
		Integer returnCode = null;
		Display display = PlatformUI.createDisplay();
		try {
			returnCode = (Integer) Subject.doAs(CurrentUser.getSubject(),
					getRunAction(display));
			if (returnCode == PlatformUI.RETURN_RESTART)
				return IApplication.EXIT_RESTART;
			else
				return IApplication.EXIT_OK;
		} catch (Exception e) {
			// e.printStackTrace();
			IStatus status = new Status(IStatus.ERROR,
					"org.eclipse.rap.security.demo", "Login failed", e);
			ErrorDialog.openError(null, "Error", "Login failed", status);
		} finally {
			display.dispose();
		}
		return returnCode;
	}

	private PrivilegedAction getRunAction(final Display display) {
		return new PrivilegedAction() {

			public Object run() {
				int result = PlatformUI.createAndRunWorkbench(display,
						new SecureWorkbenchAdvisor());
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

}
