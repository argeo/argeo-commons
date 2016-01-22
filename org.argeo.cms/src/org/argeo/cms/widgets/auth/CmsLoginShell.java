package org.argeo.cms.widgets.auth;

import org.argeo.cms.CmsView;
import org.argeo.eclipse.ui.dialogs.ErrorFeedback;
import org.eclipse.rap.rwt.RWT;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Widget;

/** The site-related user menu */
public class CmsLoginShell extends CmsLogin {
	private final Shell shell;

	public CmsLoginShell(CmsView cmsView) {
		super(cmsView);
		shell = createShell();
		shell.setData(RWT.CUSTOM_VARIANT, CMS_USER_MENU);
		createUi(shell);
	}

	/** To be overridden. */
	protected Shell createShell() {
		Shell shell = new Shell(Display.getCurrent(), SWT.NO_TRIM);
		shell.setMaximized(true);
		return shell;
	}

	/** To be overridden. */
	public void open() {
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
}
