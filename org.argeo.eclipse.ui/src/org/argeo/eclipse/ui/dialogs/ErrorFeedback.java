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
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

/** Generic error dialog to be used in try/catch blocks */
public class ErrorFeedback extends TitleAreaDialog {
	private static final long serialVersionUID = -8918084784628179044L;

	private final static Log log = LogFactory.getLog(ErrorFeedback.class);

	private final String message;
	private final Throwable exception;

	public static void show(String message, Throwable e) {
		// rethrow ThreaDeath in order to make sure that RAP will properly clean
		// up the UI thread
		if (e instanceof ThreadDeath)
			throw (ThreadDeath) e;

		new ErrorFeedback(newShell(), message, e).open();
	}

	public static void show(String message) {
		new ErrorFeedback(newShell(), message, null).open();
	}

	private static Shell newShell() {
		return new Shell(getDisplay(), SWT.NO_TRIM);
	}

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

	public ErrorFeedback(Shell parentShell, String message, Throwable e) {
		super(parentShell);
		setShellStyle(SWT.NO_TRIM);
		this.message = message;
		this.exception = e;
		log.error(message, e);
	}

	protected Point getInitialSize() {
		if (exception != null)
			return new Point(800, 600);
		else
			return new Point(400, 300);
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite dialogarea = (Composite) super.createDialogArea(parent);
		dialogarea.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		Composite composite = new Composite(dialogarea, SWT.NONE);
		composite.setLayout(new GridLayout(2, false));
		composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		setMessage(message != null ? message + (exception != null ? ": " + exception.getMessage() : "")
				: exception != null ? exception.getMessage() : "Unkown Error", IMessageProvider.ERROR);

		if (exception != null) {
			Text stack = new Text(composite, SWT.MULTI | SWT.LEAD | SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
			stack.setEditable(false);
			stack.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
			StringWriter sw = new StringWriter();
			exception.printStackTrace(new PrintWriter(sw));
			stack.setText(sw.toString());
		}

		parent.pack();
		return composite;
	}

	protected void configureShell(Shell shell) {
		super.configureShell(shell);
		shell.setText("Error");
	}
}