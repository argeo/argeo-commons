package org.argeo.security.ui.admin.commands;

import org.argeo.security.UserAdminService;
import org.argeo.security.ui.admin.views.UsersView;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.handlers.HandlerUtil;

/** Refresh the main EBI list. */
public class RefreshUsersList extends AbstractHandler {
	private UserAdminService userAdminService;

	public Object execute(ExecutionEvent event) throws ExecutionException {
		userAdminService.synchronize();
		UsersView view = (UsersView) HandlerUtil
				.getActiveWorkbenchWindow(event).getActivePage()
				.findView(UsersView.ID);
		view.refresh();
		return null;
	}

	public void setUserAdminService(UserAdminService userAdminService) {
		this.userAdminService = userAdminService;
	}

}