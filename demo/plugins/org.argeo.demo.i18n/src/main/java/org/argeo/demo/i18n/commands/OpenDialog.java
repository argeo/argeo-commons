package org.argeo.demo.i18n.commands;

import org.argeo.demo.i18n.I18nDemoMessages;
import org.argeo.demo.i18n.I18nDemoPlugin;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.MessageBox;

/**
 * Open a dummy dialog box with internationalized messages.
 */
public class OpenDialog extends AbstractHandler {

	public final static String ID = I18nDemoPlugin.ID + ".openDialog";

	public Object execute(ExecutionEvent event) throws ExecutionException {

		MessageBox mb = new MessageBox(I18nDemoPlugin.getDefault()
				.getWorkbench().getActiveWorkbenchWindow().getShell(), SWT.OK
				| SWT.CANCEL);

		// Title
		mb.setText(I18nDemoMessages.get().OpenDialog_Title);

		// Message
		mb.setMessage(I18nDemoMessages.get().OpenDialog_Message);
		mb.open();

		return null;
	}
}
