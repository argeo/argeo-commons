package org.argeo.cms.e4.rcp;

import java.security.PrivilegedExceptionAction;
import java.util.UUID;

import javax.security.auth.Subject;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import org.argeo.api.cms.CmsAuth;
import org.argeo.api.cms.CmsImageManager;
import org.argeo.api.cms.CmsView;
import org.argeo.api.cms.UxContext;
import org.argeo.cms.CmsException;
import org.argeo.cms.auth.CurrentUser;
import org.argeo.cms.swt.CmsSwtUtils;
import org.argeo.cms.swt.SimpleSwtUxContext;
import org.argeo.cms.swt.auth.CmsLoginShell;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.Platform;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.eclipse.swt.widgets.Display;

public class CmsE4Application implements IApplication, CmsView {
	private LoginContext loginContext;
	private IApplication e4Application;
	private UxContext uxContext;
	private String uid;

	@Override
	public Object start(IApplicationContext context) throws Exception {
		// TODO wait for CMS to be ready
		Thread.sleep(5000);
		
		uid = UUID.randomUUID().toString();
		Subject subject = new Subject();
		Display display = createDisplay();
		CmsLoginShell loginShell = new CmsLoginShell(this);
		// TODO customize CmsLoginShell to be smaller and centered
		loginShell.setSubject(subject);
		try {
			// try pre-auth
			loginContext = new LoginContext(CmsAuth.LOGIN_CONTEXT_SINGLE_USER, subject, loginShell);
			loginContext.login();
		} catch (LoginException e) {
			e.printStackTrace();
			loginShell.createUi();
			loginShell.open();

			while (!loginShell.getShell().isDisposed()) {
				if (!display.readAndDispatch())
					display.sleep();
			}
		}
		if (CurrentUser.getUsername(getSubject()) == null)
			throw new CmsException("Cannot log in");

		// try {
		// CallbackHandler callbackHandler = new DefaultLoginDialog(
		// display.getActiveShell());
		// loginContext = new LoginContext(
		// NodeConstants.LOGIN_CONTEXT_SINGLE_USER, subject,
		// callbackHandler);
		// } catch (LoginException e1) {
		// throw new CmsException("Cannot initialize login context", e1);
		// }
		//
		// // login
		// try {
		// loginContext.login();
		// subject = loginContext.getSubject();
		// } catch (LoginException e) {
		// e.printStackTrace();
		// display.dispose();
		// try {
		// Thread.sleep(2000);
		// } catch (InterruptedException e1) {
		// // silent
		// }
		// return null;
		// }

		uxContext = new SimpleSwtUxContext();
		//UiContext.setData(CmsView.KEY, this);
		CmsSwtUtils.registerCmsView(loginShell.getShell(), this);
		e4Application = getApplication(null);
		Object res = Subject.doAs(subject, new PrivilegedExceptionAction<Object>() {

			@Override
			public Object run() throws Exception {
				return e4Application.start(context);
			}

		});
		return res;
	}

	@Override
	public void stop() {
		if (e4Application != null)
			e4Application.stop();
	}

	static IApplication getApplication(String[] args) {
		IExtension extension = Platform.getExtensionRegistry().getExtension(Platform.PI_RUNTIME,
				Platform.PT_APPLICATIONS, "org.eclipse.e4.ui.workbench.swt.E4Application");
		try {
			IConfigurationElement[] elements = extension.getConfigurationElements();
			if (elements.length > 0) {
				IConfigurationElement[] runs = elements[0].getChildren("run");
				if (runs.length > 0) {
					Object runnable;
					runnable = runs[0].createExecutableExtension("class");
					if (runnable instanceof IApplication)
						return (IApplication) runnable;
				}
			}
		} catch (Exception e) {
			throw new IllegalStateException("Cannot find e4 application", e);
		}
		throw new IllegalStateException("Cannot find e4 application");
	}

	public static Display createDisplay() {
		Display.setAppName("Argeo CMS RCP");

		// create the display
		Display newDisplay = Display.getCurrent();
		if (newDisplay == null) {
			newDisplay = new Display();
		}
		// Set the priority higher than normal so as to be higher
		// than the JobManager.
		Thread.currentThread().setPriority(Math.min(Thread.MAX_PRIORITY, Thread.NORM_PRIORITY + 1));
		return newDisplay;
	}

	//
	// CMS VIEW
	//

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
		// if (this.loginContext != null)
		// try {
		// this.loginContext.logout();
		// } catch (LoginException e1) {
		// System.err.println("Could not log out: " + e1);
		// }
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

	@Override
	public String getUid() {
		return uid;
	}

	
}
