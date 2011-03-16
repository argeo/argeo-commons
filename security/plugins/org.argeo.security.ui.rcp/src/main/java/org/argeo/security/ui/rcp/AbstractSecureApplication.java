package org.argeo.security.ui.rcp;

import java.security.PrivilegedAction;

import javax.security.auth.Subject;
import javax.security.auth.login.LoginException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.argeo.eclipse.ui.dialogs.Error;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.application.WorkbenchAdvisor;

/**
 * Common base class for authenticated access to the Eclipse UI framework (RAP
 * and RCP)
 */
public abstract class AbstractSecureApplication implements IApplication {
	private static final Log log = LogFactory
			.getLog(AbstractSecureApplication.class);

	protected abstract WorkbenchAdvisor createWorkbenchAdvisor();

	public Object start(IApplicationContext context) throws Exception {

		Integer returnCode = null;
		Display display = PlatformUI.createDisplay();
		try {
			Subject subject = null;
			Boolean retry = true;
			while (retry) {
				try {
					SecureApplicationActivator.getLoginContext().login();
					subject = SecureApplicationActivator.getLoginContext()
							.getSubject();
					retry = false;
				} catch (LoginException e) {
					Error.show("Cannot login", e);
					retry = true;
				} catch (Exception e) {
					Error.show("Unexpected exception while trying to login", e);
					retry = false;
				}
			}

			if (subject == null) {
				// IStatus status = new Status(IStatus.ERROR,
				// "org.argeo.security.application", "Login is mandatory",
				// loginException);
				// ErrorDialog.openError(null, "Error", "Shutdown...", status);
				// return status.getSeverity();

				// TODO: log as anonymous
			}

			if (subject != null) {
				returnCode = (Integer) Subject.doAs(subject,
						getRunAction(display));
				SecureApplicationActivator.getLoginContext().logout();
				return processReturnCode(returnCode);
			} else {
				return -1;
			}
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
		// String username = CurrentUser.getUsername();
		// if (log.isDebugEnabled())
		// log.debug("workbench stopped, logged in as " + username);

	}

}
