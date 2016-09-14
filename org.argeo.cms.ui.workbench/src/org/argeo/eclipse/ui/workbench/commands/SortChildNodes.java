/*
 * Copyright (C) 2007-2012 Argeo GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.argeo.eclipse.ui.workbench.commands;

import org.argeo.cms.ui.workbench.SecurityUiPlugin;
import org.argeo.eclipse.ui.workbench.jcr.JcrBrowserView;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.Command;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.State;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.handlers.HandlerUtil;

/** Change isSorted state of the DataExplorer Browser */
public class SortChildNodes extends AbstractHandler {
	public final static String ID = SecurityUiPlugin.PLUGIN_ID
			+ ".sortChildNodes";

	public Object execute(ExecutionEvent event) throws ExecutionException {
		JcrBrowserView view = (JcrBrowserView) HandlerUtil
				.getActiveWorkbenchWindow(event).getActivePage()
				.findView(JcrBrowserView.ID);

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
