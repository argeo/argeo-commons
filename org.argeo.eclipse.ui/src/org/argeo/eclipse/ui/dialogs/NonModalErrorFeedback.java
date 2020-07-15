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

/** Generic error dialog to be used in try/catch blocks */
class NonModalErrorFeedback {
	private final static Log log = LogFactory
			.getLog(NonModalErrorFeedback.class);

	private final String message;
	private final Throwable exception;

	private Shell shell;

	public static void show(String message, Throwable e) {
		// rethrow ThreaDeath in order to make sure that RAP will properly clean
		// up the UI thread
		if (e instanceof ThreadDeath)
			throw (ThreadDeath) e;

		new NonModalErrorFeedback(getDisplay().getActiveShell(), message, e)
				.open();
	}

	public static void show(String message) {
		new NonModalErrorFeedback(getDisplay().getActiveShell(), message, null)
				.open();
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

	public NonModalErrorFeedback(Shell parentShell, String message, Throwable e) {
		this.message = message;
		this.exception = e;
		log.error(message, e);
	}

	public void open() {
		if (shell != null)
			throw new EclipseUiException("There is already a shell");
		shell = new Shell(getDisplay(), SWT.NO_TRIM | SWT.BORDER | SWT.ON_TOP);
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
	}

	protected Point getInitialSize() {
		// if (exception != null)
		// return new Point(800, 600);
		// else
		return new Point(400, 300);
	}

	protected Control createDialogArea(Composite parent) {
		Composite dialogarea = new Composite(parent, SWT.NONE);
		dialogarea.setLayout(new GridLayout());
		// Composite dialogarea = (Composite) super.createDialogArea(parent);
		dialogarea.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		Label messageLbl = new Label(dialogarea, SWT.NONE);
		if (message != null)
			messageLbl.setText(message);
		else if (exception != null)
			messageLbl.setText(exception.getLocalizedMessage());

		Composite composite = new Composite(dialogarea, SWT.NONE);
		composite.setLayout(new GridLayout(2, false));
		composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		if (exception != null) {
			Text stack = new Text(composite, SWT.MULTI | SWT.LEAD | SWT.BORDER
					| SWT.V_SCROLL | SWT.H_SCROLL);
			stack.setEditable(false);
			stack.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
			StringWriter sw = new StringWriter();
			exception.printStackTrace(new PrintWriter(sw));
			stack.setText(sw.toString());
		}

		// parent.pack();
		return composite;
	}
}