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

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;

/** Command handler to set visible or open a Argeo user. */
public class OpenArgeoUserEditor extends AbstractHandler {
	public final static String COMMAND_ID = "org.argeo.security.ui.admin.openArgeoUserEditor";
	public final static String PARAM_USERNAME = "org.argeo.security.ui.admin.username";

	public Object execute(ExecutionEvent event) throws ExecutionException {
		// try {
		// ArgeoUserEditorInput editorInput = new ArgeoUserEditorInput(
		// event.getParameter(PARAM_USERNAME));
		// IWorkbenchPage activePage = HandlerUtil.getActiveWorkbenchWindow(
		// event).getActivePage();
		// activePage.openEditor(editorInput, JcrArgeoUserEditor.ID);
		// } catch (Exception e) {
		// throw new ExecutionException("Cannot open editor", e);
		// }
		return null;
	}
}
