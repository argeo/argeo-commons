package org.argeo.security.ui.rcp;

import java.security.PrivilegedAction;

import javax.security.auth.Subject;
import javax.security.auth.login.LoginException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.argeo.OperatingSystem;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.eclipse.equinox.security.auth.ILoginContext;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.application.WorkbenchAdvisor;

/**
 * RCP workbench initialization
 */
public abstract class AbstractSecureApplication implements IApplication {
	private static final Log log = LogFactory
			.getLog(AbstractSecureApplication.class);

	protected WorkbenchAdvisor createWorkbenchAdvisor(String username) {
		return new SecureWorkbenchAdvisor(username);
	}

	public Object start(IApplicationContext context) throws Exception {
		// wait for the system to be initialized
//		try {
//			Thread.sleep(3000);
//		} catch (Exception e2) {
//			// silent
//		}

		// choose login context
		final ILoginContext loginContext;
		if (OperatingSystem.os == OperatingSystem.WINDOWS)
			loginContext = SecureApplicationActivator
					.createLoginContext(SecureApplicationActivator.CONTEXT_WINDOWS);
		else
			loginContext = SecureApplicationActivator
					.createLoginContext(SecureApplicationActivator.CONTEXT_NIX);

		final Display display = PlatformUI.createDisplay();

		// login
		Subject subject = null;
		try {
			loginContext.login();
			subject = loginContext.getSubject();
		} catch (LoginException e) {
			log.error("Error when logging in.", e);
			display.dispose();
			try {
				Thread.sleep(2000);
			} catch (InterruptedException e1) {
				// silent
			}
			return null;
		}

		// identify after successful login
		if (log.isDebugEnabled())
			log.debug("subject=" + subject);
		final String username = subject.getPrincipals().iterator().next()
				.getName();
		if (log.isDebugEnabled())
			log.debug(username + " logged in");
		display.disposeExec(new Runnable() {
			public void run() {
				log.debug("Display disposed");
				logout(loginContext, username);
			}
		});

		try {
			PrivilegedAction<?> privilegedAction = new PrivilegedAction<Object>() {
				public Object run() {
					int result = PlatformUI.createAndRunWorkbench(display,
							createWorkbenchAdvisor(username));
					return new Integer(result);
				}
			};

			Integer returnCode = (Integer) Subject.doAs(subject,
					privilegedAction);
			logout(loginContext, username);
			return processReturnCode(returnCode);
		} catch (Exception e) {
			if (subject != null)
				logout(loginContext, username);
			log.error("Unexpected error", e);
		} finally {
			display.dispose();
		}
		return null;
	}

	protected Integer processReturnCode(Integer returnCode) {
		if (returnCode == PlatformUI.RETURN_RESTART)
			return IApplication.EXIT_RESTART;
		else
			return IApplication.EXIT_OK;
	}

	static void logout(ILoginContext secureContext, String username) {
		try {
			secureContext.logout();
			log.info("Logged out " + (username != null ? username : "")
					+ " (THREAD=" + Thread.currentThread().getId() + ")");
		} catch (LoginException e) {
			log.error("Erorr when logging out", e);
		}
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
	}

}
