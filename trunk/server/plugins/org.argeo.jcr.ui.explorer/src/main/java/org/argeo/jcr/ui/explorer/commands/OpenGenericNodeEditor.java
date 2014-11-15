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
package org.argeo.jcr.ui.explorer.commands;

import org.argeo.ArgeoException;
import org.argeo.eclipse.ui.jcr.editors.NodeEditorInput;
import org.argeo.jcr.ui.explorer.JcrExplorerConstants;
import org.argeo.jcr.ui.explorer.JcrExplorerPlugin;
import org.argeo.jcr.ui.explorer.editors.GenericNodeEditor;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.handlers.HandlerUtil;

/** Opens the generic node editor. */
public class OpenGenericNodeEditor extends AbstractHandler {
	public final static String ID = JcrExplorerPlugin.ID + ".openGenericNodeEditor";

	public Object execute(ExecutionEvent event) throws ExecutionException {
		String path = event.getParameter(JcrExplorerConstants.PARAM_PATH);
		try {
			NodeEditorInput nei = new NodeEditorInput(path);
			HandlerUtil.getActiveWorkbenchWindow(event).getActivePage()
					.openEditor(nei, GenericNodeEditor.ID);
		} catch (Exception e) {
			throw new ArgeoException("Cannot open editor", e);
		}
		return null;
	}

}
