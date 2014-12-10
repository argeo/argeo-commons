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

import org.argeo.ArgeoException;
import org.argeo.eclipse.ui.jcr.editors.JcrQueryEditorInput;
import org.argeo.eclipse.ui.jcr.editors.NodeEditorInput;
import org.argeo.eclipse.ui.workbench.WorkbenchUiPlugin;
import org.argeo.eclipse.ui.workbench.jcr.GenericJcrQueryEditor;
import org.argeo.eclipse.ui.workbench.jcr.DefaultNodeEditor;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.handlers.HandlerUtil;

/** Opens an editor given its ID. */
public class OpenEditor extends AbstractHandler {
	public final static String ID = WorkbenchUiPlugin.ID + ".openEditor";

	public final static String PARAM_PATH = "param.jcrNodePath";
	public final static String PARAM_EDITOR_ID = "param.editorId";

	public Object execute(ExecutionEvent event) throws ExecutionException {
		String editorId = event.getParameter(PARAM_EDITOR_ID);
		try {
			IWorkbenchPage activePage = HandlerUtil.getActiveWorkbenchWindow(
					event).getActivePage();
			if (DefaultNodeEditor.ID.equals(editorId)) {
				String path = event.getParameter(PARAM_PATH);
				NodeEditorInput nei = new NodeEditorInput(path);
				activePage.openEditor(nei, DefaultNodeEditor.ID);
			} else if (GenericJcrQueryEditor.ID.equals(editorId)) {
				JcrQueryEditorInput editorInput = new JcrQueryEditorInput(
						GenericJcrQueryEditor.ID, null);
				activePage.openEditor(editorInput, editorId);
			}
		} catch (PartInitException e) {
			throw new ArgeoException("Cannot open editor of ID " + editorId, e);
		}
		return null;
	}
}
