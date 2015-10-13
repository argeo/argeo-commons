package org.argeo.cms.util;

import org.argeo.cms.widgets.auth.CmsLogin;
import org.eclipse.rap.rwt.RWT;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ShellAdapter;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

/** The site-related user menu */
public class UserMenu extends CmsLogin {
	private final Shell shell;

	public UserMenu(Control source, boolean autoclose) {
		super(CmsUtils.getCmsView());
		if (source != null) {
			shell = new Shell(Display.getCurrent(), SWT.NO_TRIM | SWT.BORDER
					| SWT.ON_TOP);
		} else {
			shell = new Shell(Display.getCurrent(), SWT.NO_TRIM);
			shell.setMaximized(true);
			shell.setLayout(CmsUtils.noSpaceGridLayout());
		}
		shell.setData(RWT.CUSTOM_VARIANT, CMS_USER_MENU);

		if (isAnonymous()) {
			anonymousUi(shell);
		} else {
			userUi(shell);
		}

		if (source != null) {// popup
			shell.pack();
			shell.layout();
			shell.setLocation(source.toDisplay(
					source.getSize().x - shell.getSize().x, source.getSize().y));
		} else { // centered
			Composite parent = getCredentialsBlock();
			parent.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true,
					true));
			// Rectangle shellBounds = shell.getBounds();// RAP
			// Point dialogSize = parent.getSize();
			// int x = shellBounds.x + (shellBounds.width - dialogSize.x) / 2;
			// int y = shellBounds.y + (shellBounds.height - dialogSize.y) / 2;
			// parent.setLocation(x, y);
		}
		if (autoclose)
			shell.addShellListener(new ShellAdapter() {
				private static final long serialVersionUID = 5178980294808435833L;

				@Override
				public void shellDeactivated(ShellEvent e) {
					closeShell();
				}
			});
		shell.open();

	}

	@Override
	protected void login() {
		super.login();
		closeShell();
	}

	@Override
	protected void logout() {
		closeShell();
		super.logout();
	}

	protected void closeShell() {
		shell.close();
		shell.dispose();
	}

	public Shell getShell() {
		return shell;
	}
}
