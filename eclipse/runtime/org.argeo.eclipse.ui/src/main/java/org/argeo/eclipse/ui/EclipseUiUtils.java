package org.argeo.eclipse.ui;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

/** Utilities to simplify UI development. */
public class EclipseUiUtils {
	/**
	 * Create a label and a text field for a grid layout, the text field grabing
	 * excess horizontal
	 * 
	 * @param parent
	 *            the parent composite
	 * @param label
	 *            the lable to display
	 * @param modifyListener
	 *            a {@link ModifyListener} to listen on events on the text, can
	 *            be null
	 * @return the created text
	 */
	public static Text createGridLT(Composite parent, String label,
			ModifyListener modifyListener) {
		Label lbl = new Label(parent, SWT.LEAD);
		lbl.setText(label);
		lbl.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		Text txt = new Text(parent, SWT.LEAD | SWT.BORDER);
		txt.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false));
		if (txt != null)
			txt.addModifyListener(modifyListener);
		return txt;
	}

}
