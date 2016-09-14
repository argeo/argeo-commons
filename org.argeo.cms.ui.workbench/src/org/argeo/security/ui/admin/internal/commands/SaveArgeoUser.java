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
package org.argeo.security.ui.admin.internal.commands;

import org.argeo.security.ui.SecurityUiPlugin;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.handlers.HandlerUtil;

/** Save the currently edited Argeo user. */
public class SaveArgeoUser extends AbstractHandler {
	public final static String ID = SecurityUiPlugin.PLUGIN_ID
			+ ".saveArgeoUser";

	public Object execute(ExecutionEvent event) throws ExecutionException {
		try {
			IWorkbenchPart iwp = HandlerUtil.getActiveWorkbenchWindow(event)
					.getActivePage().getActivePart();
			if (!(iwp instanceof IEditorPart))
				return null;
			IEditorPart editor = (IEditorPart) iwp;
			editor.doSave(null);
		} catch (Exception e) {
			MessageDialog.openError(Display.getDefault().getActiveShell(),
					"Error", "Cannot save user: " + e.getMessage());
		}
		return null;
	}
}