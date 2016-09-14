package org.argeo.cms.util;

import org.argeo.cms.CmsException;
import org.argeo.cms.widgets.auth.CmsLoginShell;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ShellAdapter;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

/** The site-related user menu */
public class UserMenu extends CmsLoginShell {
	private final Control source;

	public UserMenu(Control source) {
		super(CmsUtils.getCmsView());
		if (source == null)
			throw new CmsException("Source control cannot be null.");
		this.source = source;
		open();
	}

	@Override
	protected Shell createShell() {
		return new Shell(Display.getCurrent(), SWT.NO_TRIM | SWT.BORDER
				| SWT.ON_TOP);
	}

	@Override
	public void open() {
		Shell shell = getShell();
		shell.pack();
		shell.layout();
		shell.setLocation(source.toDisplay(source.getSize().x
				- shell.getSize().x, source.getSize().y));
		shell.addShellListener(new ShellAdapter() {
			private static final long serialVersionUID = 5178980294808435833L;

			@Override
			public void shellDeactivated(ShellEvent e) {
				closeShell();
			}
		});
		super.open();
	}

}
