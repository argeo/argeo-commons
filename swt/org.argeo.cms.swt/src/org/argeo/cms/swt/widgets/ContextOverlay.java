package org.argeo.cms.swt.widgets;

import org.argeo.cms.swt.CmsSwtUtils;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ShellAdapter;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

/**
 * Manages a lightweight shell which is related to a {@link Control}, typically
 * in order to reproduce a dropdown semantic, but with more flexibility.
 */
public class ContextOverlay extends ScrolledPage {
	private static final long serialVersionUID = 6702077429573324009L;

//	private Shell shell;
	private Control control;

	private int maxHeight = 400;

	public ContextOverlay(Control control, int style) {
		super(createShell(control, style), SWT.NONE);
		Shell shell = getShell();
		setLayoutData(CmsSwtUtils.fillAll());
		// TODO make autohide configurable?
		//shell.addShellListener(new AutoHideShellListener());
		this.control = control;
		control.addDisposeListener((e) -> {
			dispose();
			shell.dispose();
		});
	}

	private static Composite createShell(Control control, int style) {
		if (control == null)
			throw new IllegalArgumentException("Control cannot be null");
		if (control.isDisposed())
			throw new IllegalArgumentException("Control is disposed");
		Shell shell = new Shell(control.getShell(), SWT.NO_TRIM);
		shell.setLayout(CmsSwtUtils.noSpaceGridLayout());
		Composite placeholder = new Composite(shell, SWT.BORDER);
		placeholder.setLayoutData(CmsSwtUtils.fillAll());
		placeholder.setLayout(CmsSwtUtils.noSpaceGridLayout());
		return placeholder;
	}

	public void show() {
		Point relativeControlLocation = control.getLocation();
		Point controlLocation = control.toDisplay(relativeControlLocation.x, relativeControlLocation.y);

		int controlWidth = control.getBounds().width;

		Shell shell = getShell();

		layout(true, true);
		shell.pack();
		shell.layout(true, true);
		int targetShellWidth = shell.getSize().x < controlWidth ? controlWidth : shell.getSize().x;
		if (shell.getSize().y > maxHeight) {
			shell.setSize(targetShellWidth, maxHeight);
		} else {
			shell.setSize(targetShellWidth, shell.getSize().y);
		}

		int shellHeight = shell.getSize().y;
		int controlHeight = control.getBounds().height;
		Point shellLocation = new Point(controlLocation.x, controlLocation.y + controlHeight);
		int displayHeight = shell.getDisplay().getBounds().height;
		if (shellLocation.y + shellHeight > displayHeight) {// bottom of page
			shellLocation = new Point(controlLocation.x, controlLocation.y - shellHeight);
		}
		shell.setLocation(shellLocation);

		if (getChildren().length != 0)
			shell.open();
		if (!control.isDisposed())
			control.setFocus();
	}

	public void hide() {
		getShell().setVisible(false);
		onHide();
	}

	public boolean isShellVisible() {
		if (isDisposed())
			return false;
		return getShell().isVisible();
	}

	/** to be overridden */
	protected void onHide() {
		// does nothing by default.
	}

	private class AutoHideShellListener extends ShellAdapter {
		private static final long serialVersionUID = 7743287433907938099L;

		@Override
		public void shellDeactivated(ShellEvent e) {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e1) {
				// silent
			}
			if (!control.isDisposed() && !control.isFocusControl())
				hide();
		}
	}
}
