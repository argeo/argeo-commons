package org.argeo.security.ui.rap;

import java.security.PrivilegedAction;

import javax.security.auth.Subject;
import javax.security.auth.login.LoginException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.argeo.eclipse.ui.dialogs.Error;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.rwt.lifecycle.IEntryPoint;
import org.eclipse.rwt.service.SessionStoreEvent;
import org.eclipse.rwt.service.SessionStoreListener;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.application.IWorkbenchWindowConfigurer;
import org.eclipse.ui.application.WorkbenchAdvisor;
import org.eclipse.ui.application.WorkbenchWindowAdvisor;

public class SecureEntryPoint implements IEntryPoint, SessionStoreListener {
	private Log log = LogFactory.getLog(SecureEntryPoint.class);

	@Override
	public int createUI() {
		// log.debug("THREAD=" + Thread.currentThread().getId()
		// + ", RWT.getSessionStore().getId()="
		// + RWT.getSessionStore().getId());

		Integer returnCode = null;
		Display display = PlatformUI.createDisplay();
		try {
			Subject subject = null;
			Boolean retry = true;
			while (retry) {
				try {
					// force login in order to give Spring Security a chance to
					// load
					SecureRapActivator.getLoginContext().login();
					subject = SecureRapActivator.getLoginContext().getSubject();
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
				SecureRapActivator.getLoginContext().logout();
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

	protected Integer processReturnCode(Integer returnCode) {
		return returnCode;
	}

	protected WorkbenchAdvisor createWorkbenchAdvisor() {
		return new SecureWorkbenchAdvisor() {
			public WorkbenchWindowAdvisor createWorkbenchWindowAdvisor(
					IWorkbenchWindowConfigurer configurer) {
				return new RapSecureWorkbenchWindowAdvisor(configurer);
			}

		};
	}

	@Override
	public void beforeDestroy(SessionStoreEvent event) {
		if (log.isDebugEnabled())
			log.debug("RWT session " + event.getSessionStore().getId()
					+ " about to be destroyed. THREAD="
					+ Thread.currentThread().getId());

	}

}
