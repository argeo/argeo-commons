/*
 * Copyright (C) 2007-2012 Mathieu Baudier
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

import javax.jcr.Node;

import org.argeo.eclipse.ui.ErrorFeedback;
import org.argeo.eclipse.ui.TreeParent;
import org.argeo.jcr.ui.explorer.JcrExplorerPlugin;
import org.argeo.jcr.ui.explorer.model.SingleJcrNode;
import org.argeo.jcr.ui.explorer.model.WorkspaceNode;
import org.argeo.jcr.ui.explorer.views.GenericJcrBrowser;
import org.argeo.jcr.ui.explorer.wizards.ImportFileSystemWizard;
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
		GenericJcrBrowser view = (GenericJcrBrowser) HandlerUtil
				.getActiveWorkbenchWindow(event).getActivePage()
				.findView(HandlerUtil.getActivePartId(event));
		if (selection != null && !selection.isEmpty()
				&& selection instanceof IStructuredSelection) {
			Object obj = ((IStructuredSelection) selection).getFirstElement();
			try {
				Node folder = null;
				if (obj instanceof SingleJcrNode) {
					folder = ((SingleJcrNode) obj).getNode();
				} else if (obj instanceof WorkspaceNode) {
					folder = ((WorkspaceNode) obj).getRootNode();
				} else {
					ErrorFeedback.show(JcrExplorerPlugin
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
