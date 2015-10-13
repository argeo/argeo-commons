package org.argeo.cms.util;

import org.argeo.cms.widgets.auth.CmsLogin;
import org.eclipse.rap.rwt.RWT;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ShellAdapter;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

/** The site-related user menu */
public class UserMenu extends CmsLogin {
	private final Shell shell;

	public UserMenu(Control source, boolean autoclose) {
		super(CmsUtils.getCmsView());
		shell = new Shell(source.getDisplay(), SWT.NO_TRIM | SWT.BORDER
				| SWT.ON_TOP);
		shell.setData(RWT.CUSTOM_VARIANT, CMS_USER_MENU);

		if (isAnonymous()) {
			anonymousUi(shell);
		} else {
			userUi(shell);
		}

		shell.pack();
		shell.layout();
		if (autoclose)// popup
			shell.setLocation(source.toDisplay(
					source.getSize().x - shell.getSize().x, source.getSize().y));
		else // centered
		{
			Rectangle shellBounds = Display.getCurrent().getBounds();// RAP
			Point dialogSize = shell.getSize();
			int x = shellBounds.x + (shellBounds.width - dialogSize.x) / 2;
			int y = shellBounds.y + (shellBounds.height - dialogSize.y) / 2;
			shell.setLocation(x, y);

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
