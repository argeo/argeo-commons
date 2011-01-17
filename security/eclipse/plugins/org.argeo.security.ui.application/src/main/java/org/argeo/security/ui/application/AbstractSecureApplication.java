package org.argeo.security.ui.application;

import java.security.PrivilegedAction;

import javax.security.auth.Subject;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.argeo.security.equinox.CurrentUser;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.ErrorSupportProvider;
import org.eclipse.jface.util.Policy;
import org.eclipse.jface.window.Window.IExceptionHandler;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.application.WorkbenchAdvisor;
import org.eclipse.ui.internal.statushandlers.StackTraceSupportArea;
import org.eclipse.ui.statushandlers.WorkbenchStatusDialogManager;

public abstract class AbstractSecureApplication implements IApplication {
	private static final Log log = LogFactory
			.getLog(AbstractSecureApplication.class);

	protected abstract WorkbenchAdvisor createWorkbenchAdvisor();

	public Object start(IApplicationContext context) throws Exception {

		Integer returnCode = null;
		Display display = PlatformUI.createDisplay();

		// Force login

		try {
			String username = null;
			Exception loginException = null;
			try {
				username = CurrentUser.getUsername();
			} catch (Exception e) {
				loginException = e;
			}
			if (username == null) {
				IStatus status = new Status(IStatus.ERROR,
						"org.argeo.security.application", "Login is mandatory",
						loginException);
				ErrorDialog.openError(null, "Error", "Shutdown...", status);
				return status.getSeverity();
			}
			if (log.isDebugEnabled())
				log.debug("Logged in as " + username);
			returnCode = (Integer) Subject.doAs(CurrentUser.getSubject(),
					getRunAction(display));
			if (log.isDebugEnabled())
				log.debug("secure action completed");
			CurrentUser.logout();
			return processReturnCode(returnCode);
		} catch (Exception e) {
			// e.printStackTrace();
			IStatus status = new Status(IStatus.ERROR,
					"org.argeo.security.rcp", "Login failed", e);
			ErrorDialog.openError(null, "Error", "Shutdown...", status);
			return returnCode;
		} finally {
			display.dispose();
		}
	}

	protected Integer processReturnCode(Integer returnCode) {
		return returnCode;
	}

	@SuppressWarnings("rawtypes")
	private PrivilegedAction getRunAction(final Display display) {
		return new PrivilegedAction() {

			public Object run() {
				int result = createAndRunWorkbench(display);
				return new Integer(result);
			}
		};
	}

	protected Integer createAndRunWorkbench(Display display) {
		return PlatformUI.createAndRunWorkbench(display,
				createWorkbenchAdvisor());
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
		if (display != null && !display.isDisposed())
			display.syncExec(new Runnable() {

				public void run() {
					if (!display.isDisposed())
						workbench.close();
				}
			});

		if (log.isDebugEnabled())
			log.debug("workbench stopped");
		String username = CurrentUser.getUsername();
		if (log.isDebugEnabled())
			log.debug("workbench stopped, logged in as " + username);

	}

}
