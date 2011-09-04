package org.argeo.security.ui.rap;

import java.security.PrivilegedAction;

import javax.security.auth.Subject;
import javax.security.auth.login.LoginException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.argeo.ArgeoException;
import org.eclipse.equinox.security.auth.ILoginContext;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.rwt.RWT;
import org.eclipse.rwt.lifecycle.IEntryPoint;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.application.IWorkbenchWindowConfigurer;
import org.eclipse.ui.application.WorkbenchAdvisor;
import org.eclipse.ui.application.WorkbenchWindowAdvisor;
import org.springframework.security.BadCredentialsException;

/**
 * RAP entry point with login capabilities. On the user has been authenticated,
 * the workbench is run as a privileged action by the related subject.
 */
public class SecureEntryPoint implements IEntryPoint {
	private final static Log log = LogFactory.getLog(SecureEntryPoint.class);

	/**
	 * How many seconds to wait before invalidating the session if the user has
	 * not yet logged in.
	 */
	private Integer loginTimeout = 1 * 60;
	private Integer sessionTimeout = 15 * 60;

	@Override
	public int createUI() {
		// Short login timeout so that the modal dialog login doesn't hang
		// around too long
		RWT.getRequest().getSession().setMaxInactiveInterval(loginTimeout);

		if (log.isDebugEnabled())
			log.debug("THREAD=" + Thread.currentThread().getId()
					+ ", sessionStore=" + RWT.getSessionStore().getId());

		Integer returnCode = null;

		// create display
		Display display = PlatformUI.createDisplay();

		// log in
		final ILoginContext loginContext = SecureRapActivator
				.createLoginContext();
		Subject subject = null;
		tryLogin: while (subject == null && !display.isDisposed()) {
			try {
				loginContext.login();
				subject = loginContext.getSubject();
			} catch (LoginException e) {
				BadCredentialsException bce = wasCausedByBadCredentials(e);
				if (bce != null) {
					MessageDialog.openInformation(display.getActiveShell(),
							"Bad Credentials", bce.getMessage());
					// retry login
					continue tryLogin;
				}

				// check thread death
				ThreadDeath td = wasCausedByThreadDeath(e);
				if (td != null) {
					display.dispose();
					throw td;
				}

				if (!display.isDisposed()) {
					org.argeo.eclipse.ui.Error.show(
							"Unexpected exception during authentication", e);
					// this was not just bad credentials or death thread
					RWT.getRequest().getSession().setMaxInactiveInterval(1);
					display.dispose();
					return -1;
				} else {
					throw new ArgeoException(
							"Unexpected exception during authentication", e);
				}
			}
		}

		// identify after successful login
		if (log.isDebugEnabled())
			log.debug("Authenticated " + subject);
		final String username = subject.getPrincipals().iterator().next()
				.getName();

		// Once the user is logged in, she can have a longer session timeout
		RWT.getRequest().getSession().setMaxInactiveInterval(sessionTimeout);

		// Logout callback when the display is disposed
		display.disposeExec(new Runnable() {
			public void run() {
				log.debug("Display disposed");
				logout(loginContext, username);
			}
		});

		//
		// RUN THE WORKBENCH
		//
		try {
			returnCode = (Integer) Subject.doAs(subject, getRunAction(display));
			logout(loginContext, username);
		} finally {
			display.dispose();
		}
		return processReturnCode(returnCode);
	}

	/** Recursively look for {@link BadCredentialsException} in the root causes. */
	private BadCredentialsException wasCausedByBadCredentials(Throwable t) {
		if (t instanceof BadCredentialsException)
			return (BadCredentialsException) t;

		if (t.getCause() != null)
			return wasCausedByBadCredentials(t.getCause());
		else
			return null;
	}

	/**
	 * If there is a {@link ThreadDeath} in the root causes, rethrow it
	 * (important for RAP cleaning mechanism)
	 */
	protected ThreadDeath wasCausedByThreadDeath(Throwable t) {
		if (t instanceof ThreadDeath)
			return (ThreadDeath) t;

		if (t.getCause() != null)
			return wasCausedByThreadDeath(t.getCause());
		else
			return null;
	}

	protected void logout(ILoginContext secureContext, String username) {
		try {
			secureContext.logout();
			log.info("Logged out " + (username != null ? username : "")
					+ " (THREAD=" + Thread.currentThread().getId() + ")");
		} catch (LoginException e) {
			log.error("Erorr when logging out", e);
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

	/** To be overridden */
	protected Integer createAndRunWorkbench(Display display) {
		return PlatformUI.createAndRunWorkbench(display,
				createWorkbenchAdvisor());
	}

	/** To be overridden */
	protected Integer processReturnCode(Integer returnCode) {
		return returnCode;
	}

	/** To be overridden */
	protected WorkbenchAdvisor createWorkbenchAdvisor() {
		return new SecureWorkbenchAdvisor() {
			public WorkbenchWindowAdvisor createWorkbenchWindowAdvisor(
					IWorkbenchWindowConfigurer configurer) {
				return new RapSecureWorkbenchWindowAdvisor(configurer);
			}

		};
	}
}
