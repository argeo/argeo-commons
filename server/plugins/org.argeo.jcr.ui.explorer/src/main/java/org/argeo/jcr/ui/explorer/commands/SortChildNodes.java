package org.argeo.jcr.ui.explorer.commands;

import org.argeo.jcr.ui.explorer.JcrExplorerPlugin;
import org.argeo.jcr.ui.explorer.views.GenericJcrBrowser;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.Command;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.State;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.handlers.HandlerUtil;

/**
 * Change isSorted state of the JcrExplorer Browser
 */
public class SortChildNodes extends AbstractHandler {
	public final static String ID = JcrExplorerPlugin.ID + ".sortChildNodes";

	public Object execute(ExecutionEvent event) throws ExecutionException {
		GenericJcrBrowser view = (GenericJcrBrowser) HandlerUtil
				.getActiveWorkbenchWindow(event).getActivePage()
				.findView(GenericJcrBrowser.ID);

		ICommandService service = (ICommandService) PlatformUI.getWorkbench()
				.getService(ICommandService.class);
		Command command = service.getCommand(ID);
		State state = command.getState(ID + ".toggleState");

		boolean wasSorted = (Boolean) state.getValue();
		view.setSortChildNodes(!wasSorted);
		state.setValue(!wasSorted);
		return null;
	}
}
