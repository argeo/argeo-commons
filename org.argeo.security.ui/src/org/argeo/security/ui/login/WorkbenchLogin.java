package org.argeo.security.ui.login;

import java.security.PrivilegedAction;

import javax.security.auth.Subject;
import javax.security.auth.login.CredentialNotFoundException;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import javax.servlet.http.HttpServletRequest;

import org.argeo.ArgeoException;
import org.argeo.cms.CmsException;
import org.argeo.cms.CmsImageManager;
import org.argeo.cms.CmsView;
import org.argeo.cms.auth.AuthConstants;
import org.argeo.cms.auth.CurrentUser;
import org.argeo.cms.auth.HttpRequestCallbackHandler;
import org.argeo.cms.util.UserMenu;
import org.argeo.eclipse.ui.specific.UiContext;
import org.eclipse.rap.rwt.application.EntryPoint;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

public abstract class WorkbenchLogin implements EntryPoint, CmsView {
	// private final static Log log = LogFactory.getLog(WorkbenchLogin.class);
	private final Subject subject = new Subject();
	private LoginContext loginContext;

	@Override
	public int createUI() {
		final Display display = PlatformUI.createDisplay();
		UiContext.setData(CmsView.KEY, this);
		try {
			loginContext = new LoginContext(AuthConstants.LOGIN_CONTEXT_USER,
					subject, new HttpRequestCallbackHandler(getRequest()));
			loginContext.login();
		} catch (CredentialNotFoundException e) {
			Shell shell = new Shell(display, SWT.NO_TRIM);
			shell.setMaximized(true);
			//shell.setBackground(display.getSystemColor(SWT.COLOR_CYAN));
			UserMenu userMenu = new UserMenu(shell, false);
			shell.open();
			while (!userMenu.getShell().isDisposed()) {
				if (!display.readAndDispatch()) {
					display.sleep();
				}
			}
		} catch (LoginException e) {
			throw new ArgeoException("Cannot log in", e);
		}
		//
		// RUN THE WORKBENCH
		//
		Integer returnCode = null;
		try {
			returnCode = Subject.doAs(subject, new PrivilegedAction<Integer>() {
				public Integer run() {
					int result = createAndRunWorkbench(display,
							CurrentUser.getUsername(subject));
					return new Integer(result);
				}
			});
		} finally {
			display.dispose();
		}
		// explicit workbench closing
		logout();
		return returnCode;
	}

	protected abstract int createAndRunWorkbench(Display display,
			String username);

	// private void fullLogout() {
	// String username = CurrentUser.getUsername(subject);
	// try {
	// LoginContext loginContext = new LoginContext(
	// AuthConstants.LOGIN_CONTEXT_USER, subject);
	// loginContext.logout();
	// HttpServletRequest httpRequest = getRequest();
	// httpRequest.setAttribute(HttpContext.AUTHORIZATION, null);
	// HttpSession httpSession = httpRequest.getSession();
	// httpSession.setAttribute(HttpContext.AUTHORIZATION, null);
	// httpSession.setMaxInactiveInterval(1);
	// log.info("Logged out " + (username != null ? username : "")
	// + " (THREAD=" + Thread.currentThread().getId() + ")");
	// } catch (LoginException e) {
	// log.error("Error when logging out", e);
	// }
	// }

	protected HttpServletRequest getRequest() {
		return UiContext.getHttpRequest();
	}

	@Override
	public void navigateTo(String state) {
		// TODO Auto-generated method stub

	}

	@Override
	public void authChange(LoginContext loginContext) {
		this.loginContext = loginContext;
	}

	@Override
	public void logout() {
		if (loginContext == null)
			throw new CmsException("Login context should not bet null");
		try {
			loginContext.logout();
		} catch (LoginException e) {
			throw new CmsException("Cannot log out", e);
		}
	}

	@Override
	public final Subject getSubject() {
		return subject;
	}

	@Override
	public void exception(Throwable e) {
		// TODO Auto-generated method stub

	}

	@Override
	public CmsImageManager getImageManager() {
		// TODO Auto-generated method stub
		return null;
	}

}
