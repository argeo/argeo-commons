package org.argeo.eclipse.ui.dialogs;

import org.argeo.eclipse.ui.EclipseUiUtils;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

/** Dialog to retrieve a single value. */
public class SingleValue extends TitleAreaDialog {
	private static final long serialVersionUID = 2843538207460082349L;

	private Text valueT;
	private String value;
	private final String title, message, label;
	private final Boolean multiline;

	public static String ask(String label, String message) {
		SingleValue svd = new SingleValue(label, message);
		if (svd.open() == Window.OK)
			return svd.getString();
		else
			return null;
	}

	public static Long askLong(String label, String message) {
		SingleValue svd = new SingleValue(label, message);
		if (svd.open() == Window.OK)
			return svd.getLong();
		else
			return null;
	}

	public static Double askDouble(String label, String message) {
		SingleValue svd = new SingleValue(label, message);
		if (svd.open() == Window.OK)
			return svd.getDouble();
		else
			return null;
	}

	public SingleValue(String label, String message) {
		this(Display.getDefault().getActiveShell(), label, message, label,
				false);
	}

	public SingleValue(Shell parentShell, String title, String message,
			String label, Boolean multiline) {
		super(parentShell);
		this.title = title;
		this.message = message;
		this.label = label;
		this.multiline = multiline;
	}

	protected Point getInitialSize() {
		if (multiline)
			return new Point(450, 350);

		else
			return new Point(400, 270);
	}

	protected Control createDialogArea(Composite parent) {
		Composite dialogarea = (Composite) super.createDialogArea(parent);
		dialogarea.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		Composite composite = new Composite(dialogarea, SWT.NONE);
		composite.setLayoutData(EclipseUiUtils.fillAll());
		GridLayout layout = new GridLayout(2, false);
		layout.marginWidth = layout.marginHeight = 20;
		composite.setLayout(layout);

		valueT = createLT(composite, label);

		setMessage(message, IMessageProvider.NONE);

		parent.pack();
		valueT.setFocus();
		return composite;
	}

	@Override
	protected void okPressed() {
		value = valueT.getText();
		super.okPressed();
	}

	/** Creates label and text. */
	protected Text createLT(Composite parent, String label) {
		new Label(parent, SWT.NONE).setText(label);
		Text text;
		if (multiline) {
			text = new Text(parent, SWT.LEAD | SWT.BORDER | SWT.MULTI);
			text.setLayoutData(EclipseUiUtils.fillAll());
		} else {
			text = new Text(parent, SWT.LEAD | SWT.BORDER | SWT.SINGLE);
			text.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true));
		}
		return text;
	}

	protected void configureShell(Shell shell) {
		super.configureShell(shell);
		shell.setText(title);
	}

	public String getString() {
		return value;
	}

	public Long getLong() {
		return Long.valueOf(getString());
	}

	public Double getDouble() {
		return Double.valueOf(getString());
	}
}
