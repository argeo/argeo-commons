package org.argeo.security.ui.commands;

import org.argeo.security.ui.views.UsersView;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.handlers.HandlerUtil;

/** Refresh the main EBI list. */
public class RefreshUsersList extends AbstractHandler {

	public Object execute(ExecutionEvent event) throws ExecutionException {
		UsersView view = (UsersView) HandlerUtil
				.getActiveWorkbenchWindow(event).getActivePage()
				.findView(UsersView.ID);
		view.refresh();
		return null;
	}

}