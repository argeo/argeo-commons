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
package org.argeo.cms.ui.workbench.internal.jcr.commands;

import javax.jcr.Node;

import org.argeo.cms.ui.workbench.WorkbenchUiPlugin;
import org.argeo.cms.ui.workbench.internal.jcr.model.SingleJcrNodeElem;
import org.argeo.cms.ui.workbench.internal.jcr.model.WorkspaceElem;
import org.argeo.cms.ui.workbench.internal.jcr.parts.ImportFileSystemWizard;
import org.argeo.cms.ui.workbench.jcr.JcrBrowserView;
import org.argeo.eclipse.ui.TreeParent;
import org.argeo.eclipse.ui.dialogs.ErrorFeedback;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.ui.handlers.HandlerUtil;

/** Import a local file system directory tree. */
public class ImportFileSystem extends AbstractHandler {
	public Object execute(ExecutionEvent event) throws ExecutionException {
		ISelection selection = HandlerUtil.getActiveWorkbenchWindow(event)
				.getActivePage().getSelection();
		JcrBrowserView view = (JcrBrowserView) HandlerUtil
				.getActiveWorkbenchWindow(event).getActivePage()
				.findView(HandlerUtil.getActivePartId(event));
		if (selection != null && !selection.isEmpty()
				&& selection instanceof IStructuredSelection) {
			Object obj = ((IStructuredSelection) selection).getFirstElement();
			try {
				Node folder = null;
				if (obj instanceof SingleJcrNodeElem) {
					folder = ((SingleJcrNodeElem) obj).getNode();
				} else if (obj instanceof WorkspaceElem) {
					folder = ((WorkspaceElem) obj).getRootNode();
				} else {
					ErrorFeedback.show(WorkbenchUiPlugin
							.getMessage("warningInvalidNodeToImport"));
				}
				if (folder != null) {
					ImportFileSystemWizard wizard = new ImportFileSystemWizard(
							folder);
					WizardDialog dialog = new WizardDialog(
							HandlerUtil.getActiveShell(event), wizard);
					dialog.open();
					view.nodeAdded((TreeParent) obj);
				}
			} catch (Exception e) {
				ErrorFeedback.show("Cannot import files to " + obj, e);
			}
		}
		return null;
	}
}
