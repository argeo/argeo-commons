package org.argeo.cms.ui.workbench.commands;

import org.argeo.cms.ui.workbench.WorkbenchUiPlugin;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;

/** Utilitary command to enable sub menus in various toolbars. Does nothing */
public class DoNothing extends AbstractHandler {
	public final static String ID = WorkbenchUiPlugin.PLUGIN_ID + ".doNothing";

	public Object execute(ExecutionEvent event) throws ExecutionException {
		return null;
	}
}
