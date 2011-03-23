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

/** !Generic error dialog to be used in try/catch blocks */
public class Error extends TitleAreaDialog {
	private final static Log log = LogFactory.getLog(Error.class);

	private final String message;
	private final Throwable exception;

	public static void show(String message, Throwable e) {
		new Error(Display.getCurrent().getActiveShell(), message, e).open();
	}

	public static void show(String message) {
		new Error(Display.getCurrent().getActiveShell(), message, null).open();
	}

	public Error(Shell parentShell, String message, Throwable e) {
		super(parentShell);
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

		setMessage(message != null ? message
				+ (exception != null ? ": " + exception.getMessage() : "")
				: exception != null ? exception.getMessage() : "Unkown Error",
				IMessageProvider.ERROR);

		if (exception != null) {
			Text stack = new Text(composite, SWT.MULTI | SWT.LEAD | SWT.BORDER
					| SWT.V_SCROLL | SWT.H_SCROLL);
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
