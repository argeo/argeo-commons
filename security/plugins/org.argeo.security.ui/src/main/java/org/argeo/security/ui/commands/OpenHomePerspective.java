package org.argeo.security.ui.commands;

import org.argeo.eclipse.ui.ErrorFeedback;
import org.argeo.security.ui.UserHomePerspective;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.WorkbenchException;
import org.eclipse.ui.handlers.HandlerUtil;

/** Default action of the user menu */
public class OpenHomePerspective extends AbstractHandler {

	public Object execute(ExecutionEvent event) throws ExecutionException {
		try {
			HandlerUtil.getActiveSite(event).getWorkbenchWindow()
					.openPage(UserHomePerspective.ID, null);
		} catch (WorkbenchException e) {
			ErrorFeedback.show("Cannot open home perspective", e);
		}
		return null;
	}

}
