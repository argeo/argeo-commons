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

import java.util.Iterator;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.argeo.ArgeoException;
import org.argeo.eclipse.ui.dialogs.SingleValue;
import org.argeo.eclipse.ui.workbench.WorkbenchUiPlugin;
import org.argeo.eclipse.ui.workbench.jcr.JcrBrowserView;
import org.argeo.eclipse.ui.workbench.jcr.internal.model.SingleJcrNodeElem;
import org.argeo.jcr.JcrUtils;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.handlers.HandlerUtil;

/**
 * Canonically call JCR {@link Session#move(String, String)} on the first
 * element returned by {@link HandlerUtil#getActiveWorkbenchWindow()}
 * (...getActivePage().getSelection()), if it is a {@link SingleJcrNodeElem}.
 * The user must then fill a new name in and confirm
 */
public class RenameNode extends AbstractHandler {
	public final static String ID = WorkbenchUiPlugin.ID + ".renameNode";

	public Object execute(ExecutionEvent event) throws ExecutionException {
		IWorkbenchPage iwp = HandlerUtil.getActiveWorkbenchWindow(event)
				.getActivePage();

		ISelection selection = iwp.getSelection();
		if (selection == null || !(selection instanceof IStructuredSelection))
			return null;

		Iterator<?> lst = ((IStructuredSelection) selection).iterator();
		if (lst.hasNext()) {
			Object element = lst.next();
			if (element instanceof SingleJcrNodeElem) {
				SingleJcrNodeElem sjn = (SingleJcrNodeElem) element;
				Node node = sjn.getNode();
				Session session = null;
				String newName = null;
				String oldPath = null;
				try {
					newName = SingleValue.ask("New node name",
							"Please provide a new name for [" + node.getName()
									+ "]");
					// TODO sanity check and user feedback
					newName = JcrUtils.replaceInvalidChars(newName);
					oldPath = node.getPath();
					session = node.getSession();
					session.move(oldPath, JcrUtils.parentPath(oldPath) + "/"
							+ newName);
					session.save();

					// Manually refresh the browser view. Must be enhanced
					if (iwp.getActivePart() instanceof JcrBrowserView)
						((JcrBrowserView) iwp.getActivePart()).refresh(sjn);
				} catch (RepositoryException e) {
					throw new ArgeoException("Unable to rename " + node
							+ " to " + newName, e);
				}
			}
		}
		return null;
	}
}