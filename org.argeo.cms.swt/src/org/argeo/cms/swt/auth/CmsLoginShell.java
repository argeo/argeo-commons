package org.argeo.cms.swt.auth;

import org.argeo.api.cms.CmsView;
import org.argeo.cms.swt.CmsSwtUtils;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

/** The site-related user menu */
public class CmsLoginShell extends CmsLogin {
	private final Shell shell;

	public CmsLoginShell(CmsView cmsView) {
		super(cmsView);
		shell = createShell();
//		createUi(shell);
	}

	/** To be overridden. */
	protected Shell createShell() {
		Shell shell = new Shell(Display.getCurrent(), SWT.NO_TRIM);
		shell.setMaximized(true);
		return shell;
	}

	/** To be overridden. */
	public void open() {
		CmsSwtUtils.style(shell, CMS_USER_MENU);
		shell.open();
	}

	@Override
	protected boolean login() {
		boolean success = false;
		try {
			success = super.login();
			return success;
		} finally {
			if (success)
				closeShell();
			else {
				for (Control child : shell.getChildren())
					child.dispose();
				createUi(shell);
				shell.layout();
				// TODO error message
			}
		}
	}

	@Override
	protected void logout() {
		closeShell();
		super.logout();
	}

	protected void closeShell() {
		if (!shell.isDisposed()) {
			shell.close();
			shell.dispose();
		}
	}

	public Shell getShell() {
		return shell;
	}
	
	public void createUi(){
		createUi(shell);
	}
}
