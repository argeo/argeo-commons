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
import org.argeo.cms.widgets.auth.CmsLoginShell;
import org.argeo.eclipse.ui.specific.UiContext;
import org.eclipse.rap.rwt.application.EntryPoint;
import org.eclipse.swt.widgets.Display;
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
			// try pre-auth
			loginContext = new LoginContext(AuthConstants.LOGIN_CONTEXT_USER,
					subject, new HttpRequestCallbackHandler(getRequest()));
			loginContext.login();
		} catch (CredentialNotFoundException e) {
			CmsLoginShell loginShell = createCmsLoginShell();
			loginShell.open();
			while (!loginShell.getShell().isDisposed()) {
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
			// explicit workbench closing
			logout();
		} finally {
			display.dispose();
		}
		return returnCode;
	}

	protected abstract int createAndRunWorkbench(Display display,
			String username);

	protected HttpServletRequest getRequest() {
		return UiContext.getHttpRequest();
	}

	protected CmsLoginShell createCmsLoginShell() {
		return new CmsLoginShell(this);
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
