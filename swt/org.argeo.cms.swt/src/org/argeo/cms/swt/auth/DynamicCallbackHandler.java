package org.argeo.cms.swt.auth;

import java.io.IOException;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;

import org.argeo.cms.swt.dialogs.LightweightDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

public class DynamicCallbackHandler implements CallbackHandler {

	@Override
	public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
		Shell activeShell = Display.getCurrent().getActiveShell();
		LightweightDialog dialog = new LightweightDialog(activeShell) {

			@Override
			protected Control createDialogArea(Composite parent) {
				CompositeCallbackHandler cch = new CompositeCallbackHandler(parent, SWT.NONE);
				cch.createCallbackHandlers(callbacks);
				return cch;
			}
		};
		dialog.setBlockOnOpen(true);
		dialog.open();
	}

}
