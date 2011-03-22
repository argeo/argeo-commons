package org.argeo.security.ui.commands;

import org.argeo.security.ui.dialogs.ChangePasswordDialog;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.handlers.HandlerUtil;
import org.springframework.security.userdetails.UserDetailsManager;

/** Opens the change password dialog. */
public class OpenChangePasswordDialog extends AbstractHandler {
	private UserDetailsManager userDetailsManager;

	public Object execute(ExecutionEvent event) throws ExecutionException {
		ChangePasswordDialog dialog = new ChangePasswordDialog(
				HandlerUtil.getActiveShell(event), userDetailsManager);
		dialog.open();
		return null;
	}

	public void setUserDetailsManager(UserDetailsManager userDetailsManager) {
		this.userDetailsManager = userDetailsManager;
	}

}
