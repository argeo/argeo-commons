package org.argeo.cms.e4.rap;

import java.security.AccessController;

import javax.security.auth.Subject;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import org.argeo.cms.CmsException;
import org.argeo.cms.auth.CurrentUser;
import org.argeo.cms.ui.CmsImageManager;
import org.argeo.cms.ui.CmsView;
import org.argeo.cms.ui.UxContext;
import org.argeo.cms.util.SimpleUxContext;
import org.argeo.cms.widgets.auth.CmsLoginShell;
import org.argeo.node.NodeConstants;
import org.eclipse.e4.ui.workbench.lifecycle.PostContextCreate;
import org.eclipse.swt.widgets.Display;

@SuppressWarnings("restriction")
public class CmsLoginLifecycle implements CmsView {
	private UxContext uxContext;
	private LoginContext loginContext;

	@PostContextCreate
	boolean login(Display d) {
		Subject subject = Subject.getSubject(AccessController.getContext());
		Display display = Display.getCurrent();
		CmsLoginShell loginShell = new CmsLoginShell(this);
		loginShell.setSubject(subject);
		try {
			// try pre-auth
			loginContext = new LoginContext(NodeConstants.LOGIN_CONTEXT_USER, subject, loginShell);
			loginContext.login();
		} catch (LoginException e) {
			loginShell.createUi();
			loginShell.open();

			while (!loginShell.getShell().isDisposed()) {
				if (!display.readAndDispatch())
					display.sleep();
			}
		}
		if (CurrentUser.getUsername(getSubject()) == null)
			return false;
		uxContext = new SimpleUxContext();
		return true;
	}

	@Override
	public UxContext getUxContext() {
		return uxContext;
	}

	@Override
	public void navigateTo(String state) {
		// TODO Auto-generated method stub

	}

	@Override
	public void authChange(LoginContext loginContext) {
		if (loginContext == null)
			throw new CmsException("Login context cannot be null");
		// logout previous login context
//		if (this.loginContext != null)
//			try {
//				this.loginContext.logout();
//			} catch (LoginException e1) {
//				System.err.println("Could not log out: " + e1);
//			}
		this.loginContext = loginContext;
	}

	@Override
	public void logout() {
		if (loginContext == null)
			throw new CmsException("Login context should not bet null");
		try {
			CurrentUser.logoutCmsSession(loginContext.getSubject());
			loginContext.logout();
		} catch (LoginException e) {
			throw new CmsException("Cannot log out", e);
		}
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

	protected Subject getSubject() {
		return loginContext.getSubject();
	}

	@Override
	public boolean isAnonymous() {
		return CurrentUser.isAnonymous(getSubject());
	}

}
