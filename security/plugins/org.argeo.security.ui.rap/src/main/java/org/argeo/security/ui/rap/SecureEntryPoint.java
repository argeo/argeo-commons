package org.argeo.security.ui.rap;

import java.security.PrivilegedAction;

import javax.security.auth.Subject;
import javax.security.auth.login.LoginException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.equinox.security.auth.ILoginContext;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.rwt.RWT;
import org.eclipse.rwt.lifecycle.IEntryPoint;
import org.eclipse.rwt.service.SessionStoreEvent;
import org.eclipse.rwt.service.SessionStoreListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.application.IWorkbenchWindowConfigurer;
import org.eclipse.ui.application.WorkbenchAdvisor;
import org.eclipse.ui.application.WorkbenchWindowAdvisor;

public class SecureEntryPoint implements IEntryPoint, SessionStoreListener {
	private final static Log log = LogFactory.getLog(SecureEntryPoint.class);

	@SuppressWarnings("unchecked")
	@Override
	public int createUI() {
		// 15 mins session timeout
		RWT.getRequest().getSession().setMaxInactiveInterval(15 * 60);

		if (log.isDebugEnabled())
			log.debug("THREAD=" + Thread.currentThread().getId()
					+ ", sessionStore=" + RWT.getSessionStore().getId());

		final ILoginContext loginContext = SecureRapActivator
				.createLoginContext();
		Integer returnCode = null;
		Display display = PlatformUI.createDisplay();

		Subject subject = null;
		try {
			loginContext.login();
			subject = loginContext.getSubject();
		} catch (LoginException e) {
			log.error("Error when logging in.", e);
			MessageDialog.openInformation(display.getActiveShell(),
					"Login failed", "Login failed");
			display.dispose();
			RWT.getRequest().getSession().setMaxInactiveInterval(1);
			try {
				Thread.sleep(2000);
			} catch (InterruptedException e1) {
				// silent
			}
			// throw new RuntimeException("Login failed", e);
			return -1;
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
				// invalidate session
				RWT.getRequest().getSession().setMaxInactiveInterval(1);
				try {
					Thread.sleep(2000);
				} catch (InterruptedException e1) {
					// silent
				}
			}
		});

		try {
			returnCode = (Integer) Subject.doAs(subject, getRunAction(display));
			loginContext.logout();
			return processReturnCode(returnCode);
		} catch (Exception e) {
			if (subject != null)
				logout(loginContext, username);
			// RWT.getRequest().getSession().setMaxInactiveInterval(1);
			log.error("Unexpected error", e);
			// throw new ArgeoException("Cannot login", e);
		} finally {
			display.dispose();
		}
		return -1;
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

	// static void closeWorkbench() {
	// final IWorkbench workbench;
	// try {
	// workbench = PlatformUI.getWorkbench();
	// } catch (Exception e) {
	// return;
	// }
	// if (workbench == null)
	// return;
	// final Display display = workbench.getDisplay();
	// if (display != null && !display.isDisposed())
	// display.syncExec(new Runnable() {
	//
	// public void run() {
	// if (!display.isDisposed())
	// workbench.close();
	// }
	// });
	//
	// if (log.isDebugEnabled())
	// log.debug("Workbench closed");
	// }

	static class FailedLogin extends MessageDialog {

		public FailedLogin(Shell parentShell, String dialogTitle,
				Image dialogTitleImage, String dialogMessage,
				int dialogImageType, String[] dialogButtonLabels,
				int defaultIndex) {
			super(parentShell, "Failed ", dialogTitleImage, dialogMessage,
					dialogImageType, dialogButtonLabels, defaultIndex);
			// TODO Auto-generated constructor stub
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
