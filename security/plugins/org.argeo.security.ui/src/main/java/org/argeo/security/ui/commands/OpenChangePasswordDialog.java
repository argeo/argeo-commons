package org.argeo.security.ui.commands;

import org.argeo.security.CurrentUserService;
import org.argeo.security.ui.dialogs.ChangePasswordDialog;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.handlers.HandlerUtil;

/** Opens the change password dialog. */
public class OpenChangePasswordDialog extends AbstractHandler {
	private CurrentUserService currentUserService;

	public Object execute(ExecutionEvent event) throws ExecutionException {
		ChangePasswordDialog dialog = new ChangePasswordDialog(
				HandlerUtil.getActiveShell(event), currentUserService);
		dialog.open();
		return null;
	}

	public void setCurrentUserService(CurrentUserService currentUserService) {
		this.currentUserService = currentUserService;
	}

}
