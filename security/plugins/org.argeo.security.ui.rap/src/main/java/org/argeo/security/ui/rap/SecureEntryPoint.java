package org.argeo.security.ui.rap;

import java.security.PrivilegedAction;

import javax.security.auth.Subject;
import javax.security.auth.login.LoginException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.argeo.ArgeoException;
import org.argeo.eclipse.ui.ErrorFeedback;
import org.eclipse.equinox.security.auth.ILoginContext;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.rwt.RWT;
import org.eclipse.rwt.lifecycle.IEntryPoint;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.springframework.security.BadCredentialsException;
import org.springframework.security.context.SecurityContext;
import org.springframework.security.context.SecurityContextHolder;

/**
 * RAP entry point with login capabilities. Once the user has been
 * authenticated, the workbench is run as a privileged action by the related
 * subject.
 */
public class SecureEntryPoint implements IEntryPoint {
	private final static Log log = LogFactory.getLog(SecureEntryPoint.class);

	/**
	 * From org.springframework.security.context.
	 * HttpSessionContextIntegrationFilter
	 */
	protected static final String SPRING_SECURITY_CONTEXT_KEY = "SPRING_SECURITY_CONTEXT";

	/**
	 * How many seconds to wait before invalidating the session if the user has
	 * not yet logged in.
	 */
	private Integer loginTimeout = 1 * 60;
	// TODO make it configurable
	/** Default session timeout is 8 hours (European working day length) */
	private Integer sessionTimeout = 8 * 60 * 60;

	@Override
	public int createUI() {
		// Short login timeout so that the modal dialog login doesn't hang
		// around too long
		RWT.getRequest().getSession().setMaxInactiveInterval(loginTimeout);

		HttpServletRequest httpRequest = RWT.getRequest();
		HttpSession httpSession = httpRequest.getSession();
		Object contextFromSessionObject = httpSession
				.getAttribute(SPRING_SECURITY_CONTEXT_KEY);
		if (contextFromSessionObject != null)
			SecurityContextHolder
					.setContext((SecurityContext) contextFromSessionObject);

		if (log.isDebugEnabled())
			log.debug("THREAD=" + Thread.currentThread().getId()
					+ ", sessionStore=" + RWT.getSessionStore().getId()
					+ ", remote user=" + httpRequest.getRemoteUser());

		// create display
		final Display display = PlatformUI.createDisplay();

		// log in
		final ILoginContext loginContext = SecureRapActivator
				.createLoginContext(SecureRapActivator.CONTEXT_SPRING);
		Subject subject = null;
		tryLogin: while (subject == null && !display.isDisposed()) {
			try {
				loginContext.login();
				subject = loginContext.getSubject();

				if (httpSession.getAttribute(SPRING_SECURITY_CONTEXT_KEY) == null)
					httpSession.setAttribute(SPRING_SECURITY_CONTEXT_KEY,
							SecurityContextHolder.getContext());

				// Once the user is logged in, she can have a longer session
				// timeout
				RWT.getRequest().getSession()
						.setMaxInactiveInterval(sessionTimeout);
				if (log.isDebugEnabled())
					log.debug("Authenticated " + subject);
			} catch (LoginException e) {
				BadCredentialsException bce = wasCausedByBadCredentials(e);
				if (bce != null) {
					MessageDialog.openInformation(display.getActiveShell(),
							"Bad Credentials", bce.getMessage());
					// retry login
					continue tryLogin;
				}
				return processLoginDeath(display, e);
			}
		}

		final String username = subject.getPrincipals().iterator().next()
				.getName();
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
		Integer returnCode = null;
		try {
			returnCode = Subject.doAs(subject, new PrivilegedAction<Integer>() {
				public Integer run() {
					RapWorkbenchAdvisor workbenchAdvisor = new RapWorkbenchAdvisor(
							username);
					int result = PlatformUI.createAndRunWorkbench(display,
							workbenchAdvisor);
					return new Integer(result);
				}
			});
			logout(loginContext, username);
		} finally {
			display.dispose();
		}
		return returnCode;
	}

	private Integer processLoginDeath(Display display, LoginException e) {
		// check thread death
		ThreadDeath td = wasCausedByThreadDeath(e);
		if (td != null) {
			display.dispose();
			throw td;
		}
		if (!display.isDisposed()) {
			ErrorFeedback.show("Unexpected exception during authentication", e);
			// this was not just bad credentials or death thread
			RWT.getRequest().getSession().setMaxInactiveInterval(1);
			display.dispose();
			return -1;
		} else {
			throw new ArgeoException(
					"Unexpected exception during authentication", e);
		}

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
}
