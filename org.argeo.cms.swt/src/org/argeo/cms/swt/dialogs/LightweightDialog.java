package org.argeo.cms.swt.dialogs;

import org.argeo.api.cms.CmsLog;
import org.argeo.eclipse.ui.EclipseUiException;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.ShellAdapter;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

/** Generic lightweight dialog, not based on JFace. */
public class LightweightDialog {
	private final static CmsLog log = CmsLog.getLog(LightweightDialog.class);

	// must be the same value as org.eclipse.jface.window.Window#OK
	public final static int OK = 0;
	// must be the same value as org.eclipse.jface.window.Window#CANCEL
	public final static int CANCEL = 1;

	private Shell parentShell;
	private Shell backgroundShell;
	private Shell foregoundShell;

	private Integer returnCode = null;
	private boolean block = true;

	private String title;

	/** Tries to find a display */
	private static Display getDisplay() {
		try {
			Display display = Display.getCurrent();
			if (display != null)
				return display;
			else
				return Display.getDefault();
		} catch (Exception e) {
			return Display.getCurrent();
		}
	}

	public LightweightDialog(Shell parentShell) {
		this.parentShell = parentShell;
	}

	public int open() {
		if (foregoundShell != null)
			throw new EclipseUiException("There is already a shell");
		backgroundShell = new Shell(parentShell, SWT.ON_TOP);
		backgroundShell.setFullScreen(true);
		// if (parentShell != null) {
		// backgroundShell.setBounds(parentShell.getBounds());
		// } else
		// backgroundShell.setMaximized(true);
		backgroundShell.setAlpha(128);
		backgroundShell.setBackground(getDisplay().getSystemColor(SWT.COLOR_BLACK));
		foregoundShell = new Shell(backgroundShell, SWT.NO_TRIM | SWT.ON_TOP);
		if (title != null)
			setTitle(title);
		foregoundShell.setLayout(new GridLayout());
		foregoundShell.setSize(getInitialSize());
		createDialogArea(foregoundShell);
		// shell.pack();
		// shell.layout();

		Rectangle shellBounds = parentShell != null ? parentShell.getBounds() : Display.getCurrent().getBounds();// RAP
		Point dialogSize = foregoundShell.getSize();
		int x = shellBounds.x + (shellBounds.width - dialogSize.x) / 2;
		int y = shellBounds.y + (shellBounds.height - dialogSize.y) / 2;
		foregoundShell.setLocation(x, y);

		foregoundShell.addShellListener(new ShellAdapter() {
			private static final long serialVersionUID = -2701270481953688763L;

			@Override
			public void shellDeactivated(ShellEvent e) {
				if (hasChildShells())
					return;
				if (returnCode == null)// not yet closed
					closeShell(CANCEL);
			}

			@Override
			public void shellClosed(ShellEvent e) {
				notifyClose();
			}

		});

		backgroundShell.open();
		foregoundShell.open();
		// after the foreground shell has been opened
		backgroundShell.addFocusListener(new FocusListener() {
			private static final long serialVersionUID = 3137408447474661070L;

			@Override
			public void focusLost(FocusEvent event) {
			}

			@Override
			public void focusGained(FocusEvent event) {
				if (hasChildShells())
					return;
				if (returnCode == null)// not yet closed
					closeShell(CANCEL);
			}
		});

		if (block) {
			block();
		}
		if (returnCode == null)
			returnCode = OK;
		return returnCode;
	}

	public void block() {
		try {
			runEventLoop(foregoundShell);
		} catch (ThreadDeath t) {
			returnCode = CANCEL;
			if (log.isTraceEnabled())
				log.error("Thread death, canceling dialog", t);
		} catch (Throwable t) {
			returnCode = CANCEL;
			log.error("Cannot open blocking lightweight dialog", t);
		}
	}

	private boolean hasChildShells() {
		if (foregoundShell == null)
			return false;
		return foregoundShell.getShells().length != 0;
	}

	// public synchronized int openAndWait() {
	// open();
	// while (returnCode == null)
	// try {
	// wait(100);
	// } catch (InterruptedException e) {
	// // silent
	// }
	// return returnCode;
	// }

	private synchronized void notifyClose() {
		if (returnCode == null)
			returnCode = CANCEL;
		notifyAll();
	}

	protected void closeShell(int returnCode) {
		this.returnCode = returnCode;
		if (CANCEL == returnCode)
			onCancel();
		if (foregoundShell != null && !foregoundShell.isDisposed()) {
			foregoundShell.close();
			foregoundShell.dispose();
			foregoundShell = null;
		}

		if (backgroundShell != null && !backgroundShell.isDisposed()) {
			backgroundShell.close();
			backgroundShell.dispose();
		}
	}

	protected Point getInitialSize() {
		// if (exception != null)
		// return new Point(800, 600);
		// else
		return new Point(600, 400);
	}

	protected Control createDialogArea(Composite parent) {
		Composite dialogarea = new Composite(parent, SWT.NONE);
		dialogarea.setLayout(new GridLayout());
		dialogarea.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		return dialogarea;
	}

	protected Shell getBackgroundShell() {
		return backgroundShell;
	}

	protected Shell getForegoundShell() {
		return foregoundShell;
	}

	public void setBlockOnOpen(boolean shouldBlock) {
		block = shouldBlock;
	}

	public void pack() {
		foregoundShell.pack();
	}

	private void runEventLoop(Shell loopShell) {
		Display display;
		if (foregoundShell == null) {
			display = Display.getCurrent();
		} else {
			display = loopShell.getDisplay();
		}

		while (loopShell != null && !loopShell.isDisposed()) {
			try {
				if (!display.readAndDispatch()) {
					display.sleep();
				}
			} catch (UnsupportedOperationException e) {
				throw e;
			} catch (Throwable e) {
				handleException(e);
			}
		}
		if (!display.isDisposed())
			display.update();
	}

	protected void handleException(Throwable t) {
		if (t instanceof ThreadDeath) {
			// Don't catch ThreadDeath as this is a normal occurrence when
			// the thread dies
			throw (ThreadDeath) t;
		}
		// Try to keep running.
		t.printStackTrace();
	}

	/** @return false, if the dialog should not be closed. */
	protected boolean onCancel() {
		return true;
	}

	public void setTitle(String title) {
		this.title = title;
		if (title != null && getForegoundShell() != null)
			getForegoundShell().setText(title);
	}

	public Integer getReturnCode() {
		return returnCode;
	}

}