package org.argeo.cms.swt.dialogs;

import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

/** A dialog asking a for a single value. */
public class SingleValueDialog extends CmsMessageDialog {
	private Text valueT;
	private String value;
	private String defaultValue;

	public SingleValueDialog(Shell parentShell, String message) {
		super(parentShell, message, QUESTION);
	}

	public SingleValueDialog(Shell parentShell, String message, String defaultValue) {
		super(parentShell, message, QUESTION);
		this.defaultValue = defaultValue;
	}

	@Override
	protected Control createInputArea(Composite parent) {
		valueT = new Text(parent, SWT.LEAD | SWT.BORDER | SWT.SINGLE);
		valueT.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true));
		if (defaultValue != null)
			valueT.setText(defaultValue);
		return valueT;
	}

	@Override
	protected void okPressed() {
		value = valueT.getText();
		super.okPressed();
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

	public static String ask(String message) {
		return ask(message, null);
	}

	public static String ask(String message, String defaultValue) {
		SingleValueDialog svd = new SingleValueDialog(Display.getCurrent().getActiveShell(), message, defaultValue);
		if (svd.open() == Window.OK)
			return svd.getString();
		else
			return null;
	}

	public static Long askLong(String message) {
		SingleValueDialog svd = new SingleValueDialog(Display.getCurrent().getActiveShell(), message);
		if (svd.open() == Window.OK)
			return svd.getLong();
		else
			return null;
	}

	public static Double askDouble(String message) {
		SingleValueDialog svd = new SingleValueDialog(Display.getCurrent().getActiveShell(), message);
		if (svd.open() == Window.OK)
			return svd.getDouble();
		else
			return null;
	}

}
