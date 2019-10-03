package org.eclipse.rap.rwt.application;

import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

public abstract class AbstractEntryPoint implements EntryPoint {
	private Display display;
	private Shell shell;

	protected Shell createShell(Display display) {
		return new Shell(display);
	}

	protected void createContents(Composite parent) {

	}

	public int createUI() {
		display = new Display();
		shell = createShell(display);
		shell.setLayout(new GridLayout(1, false));
		createContents(shell);
		if (shell.getMaximized()) {
			shell.layout();
		} else {
			shell.pack();
		}
		shell.open();
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch()) {
				display.sleep();
			}
		}
		display.dispose();
		return 0;
	}

	protected Shell getShell() {
		return shell;
	}
}
