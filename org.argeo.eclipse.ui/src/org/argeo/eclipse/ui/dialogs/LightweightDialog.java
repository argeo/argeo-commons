/*
 * Copyright (C) 2007-2012 Argeo GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.argeo.eclipse.ui.dialogs;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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
	private final static Log log = LogFactory.getLog(LightweightDialog.class);

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
		backgroundShell = new Shell(parentShell, SWT.DIALOG_TRIM | SWT.ON_TOP);
		backgroundShell.setFullScreen(true);
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

		Rectangle shellBounds = Display.getCurrent().getBounds();// RAP
		Point dialogSize = foregoundShell.getSize();
		int x = shellBounds.x + (shellBounds.width - dialogSize.x) / 2;
		int y = shellBounds.y + (shellBounds.height - dialogSize.y) / 2;
		foregoundShell.setLocation(x, y);

		foregoundShell.addShellListener(new ShellAdapter() {
			private static final long serialVersionUID = -2701270481953688763L;

			@Override
			public void shellDeactivated(ShellEvent e) {
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
				if (returnCode == null)// not yet closed
					closeShell(CANCEL);
			}
		});

		if (block) {
			try {
				runEventLoop(foregoundShell);
			} catch (Throwable t) {
				log.error("Cannot open blocking lightweight dialog", t);
			}
		}
		if (returnCode == null)
			returnCode = OK;
		return returnCode;
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

}