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

import java.io.PrintWriter;
import java.io.StringWriter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.argeo.eclipse.ui.EclipseUiException;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ShellAdapter;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

/** Generic lightweight dialog, not based on JFace. */
public class LightweightDialog {
	private final static Log log = LogFactory.getLog(LightweightDialog.class);

	private Shell parentShell;
	private Shell backgroundShell;
	private Shell shell;

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

	public void open() {
		if (shell != null)
			throw new EclipseUiException("There is already a shell");
		backgroundShell = new Shell(parentShell,SWT.NO_TRIM | SWT.BORDER | SWT.ON_TOP);
		backgroundShell.setMaximized(true);
		backgroundShell.setAlpha(128);
		backgroundShell.setBackground(getDisplay().getSystemColor(SWT.COLOR_BLACK));
		backgroundShell.open();
		shell = new Shell(backgroundShell, SWT.NO_TRIM | SWT.BORDER | SWT.ON_TOP);
		shell.setLayout(new GridLayout());
		// shell.setText("Error");
		shell.setSize(getInitialSize());
		createDialogArea(shell);
		// shell.pack();
		// shell.layout();

		Rectangle shellBounds = Display.getCurrent().getBounds();// RAP
		Point dialogSize = shell.getSize();
		int x = shellBounds.x + (shellBounds.width - dialogSize.x) / 2;
		int y = shellBounds.y + (shellBounds.height - dialogSize.y) / 2;
		shell.setLocation(x, y);

		shell.addShellListener(new ShellAdapter() {
			private static final long serialVersionUID = -2701270481953688763L;

			@Override
			public void shellDeactivated(ShellEvent e) {
				closeShell();
			}
		});

		shell.open();
	}

	protected void closeShell() {
		shell.close();
		shell.dispose();
		shell = null;
		
		backgroundShell.close();
		backgroundShell.dispose();
	}

	protected Point getInitialSize() {
		// if (exception != null)
		// return new Point(800, 600);
		// else
		return new Point(400, 400);
	}

	protected Control createDialogArea(Composite parent) {
		Composite dialogarea = new Composite(parent, SWT.NONE);
		dialogarea.setLayout(new GridLayout());
		dialogarea.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		return dialogarea;
	}
}