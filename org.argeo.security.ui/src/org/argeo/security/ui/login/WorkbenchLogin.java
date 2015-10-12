package org.argeo.security.ui.login;

import java.security.PrivilegedAction;

import javax.security.auth.Subject;

import org.argeo.cms.CmsImageManager;
import org.argeo.cms.CmsView;
import org.argeo.cms.auth.CurrentUser;
import org.argeo.cms.util.UserMenu;
import org.eclipse.rap.rwt.application.EntryPoint;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

public abstract class WorkbenchLogin implements EntryPoint, CmsView {
	private final Subject subject = new Subject();

	@Override
	public int createUI() {
		final Display display = PlatformUI.createDisplay();
		display.setData(CmsView.KEY, this);
		Shell shell = new Shell(display, SWT.NO_TRIM);
		shell.setMaximized(true);
		UserMenu userMenu = new UserMenu(shell, false);
		shell.open();
		while (!userMenu.getShell().isDisposed()) {
			if (!display.readAndDispatch()) {
				display.sleep();
			}
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
		return returnCode;
		// display.dispose();
		// return 0;
	}

	protected abstract int createAndRunWorkbench(Display display,
			String username);

	@Override
	public void navigateTo(String state) {
		// TODO Auto-generated method stub

	}

	@Override
	public void authChange() {
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
