package org.argeo.security.ui.commands;

import org.argeo.security.ui.dialogs.ChangePasswordDialog;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.ui.handlers.HandlerUtil;
import org.springframework.security.userdetails.UserDetailsManager;

/** Opens the change password dialog. */
public class OpenChangePasswordDialog extends AbstractHandler {
	private UserDetailsManager userDetailsManager;

	public Object execute(ExecutionEvent event) throws ExecutionException {
		ChangePasswordDialog dialog = new ChangePasswordDialog(
				HandlerUtil.getActiveShell(event), userDetailsManager);
		if (dialog.open() == Dialog.OK) {
			MessageDialog.openInformation(HandlerUtil.getActiveShell(event),
					"Password changed", "Password changed.");
		}
		return null;
	}

	public void setUserDetailsManager(UserDetailsManager userDetailsManager) {
		this.userDetailsManager = userDetailsManager;
	}

}
