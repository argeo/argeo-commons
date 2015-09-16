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
package org.argeo.security.ui.admin.commands;

import org.argeo.security.ui.admin.internal.parts.GroupsView;
import org.argeo.security.ui.admin.internal.parts.UsersView;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;

/** Retrieve the active view or editor and call forceRefresh method if defined */
public class ForceRefresh extends AbstractHandler {

	public Object execute(ExecutionEvent event) throws ExecutionException {
		IWorkbenchWindow iww = HandlerUtil.getActiveWorkbenchWindow(event);
		if (iww == null)
			return null;
		IWorkbenchPage activePage = iww.getActivePage();
		IWorkbenchPart part = activePage.getActivePart();
		if (part instanceof UsersView)
			((UsersView) part).refresh();
		else if (part instanceof GroupsView)
			((GroupsView) part).refresh();
		return null;
	}
}