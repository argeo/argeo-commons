package org.argeo.cms.ui;

import javax.security.auth.login.LoginContext;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;

/** Provides interaction with the CMS system. */
public interface CmsView {
	//String KEY = "org.argeo.cms.ui.view";

	UxContext getUxContext();

	// NAVIGATION
	void navigateTo(String state);

	// SECURITY
	void authChange(LoginContext loginContext);

	void logout();

	// void registerCallbackHandler(CallbackHandler callbackHandler);

	// SERVICES
	void exception(Throwable e);

	CmsImageManager getImageManager();

	boolean isAnonymous();

	static CmsView getCmsView(Composite parent) {
		// find parent shell
		Shell topShell = parent.getShell();
		while (topShell.getParent() != null)
			topShell = (Shell) topShell.getParent();
		return (CmsView) topShell.getData(CmsView.class.getName());
	}

	static void registerCmsView(Shell shell, CmsView view) {
		// find parent shell
		Shell topShell = shell;
		while (topShell.getParent() != null)
			topShell = (Shell) topShell.getParent();
		// check if already set
		if (topShell.getData(CmsView.class.getName()) != null) {
			CmsView registeredView = (CmsView) topShell.getData(CmsView.class.getName());
			throw new IllegalArgumentException(
					"Cms view " + registeredView + " already registered in this shell");
		}
		shell.setData(CmsView.class.getName(), view);
	}

}
