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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.jcr.Node;

import org.argeo.cms.ui.jcr.model.SingleJcrNodeElem;
import org.argeo.cms.ui.jcr.model.WorkspaceElem;
import org.argeo.cms.ui.workbench.WorkbenchUiPlugin;
import org.argeo.eclipse.ui.dialogs.ErrorFeedback;
import org.argeo.jcr.JcrUtils;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.handlers.HandlerUtil;

/** Compute an approximative size for the selected node(s) */
public class GetNodeSize extends AbstractHandler {
	// private final static Log log = LogFactory.getLog(GetNodeSize.class);

	public final static String ID = WorkbenchUiPlugin.PLUGIN_ID + ".getNodeSize";

	public Object execute(ExecutionEvent event) throws ExecutionException {

		ISelection selection = HandlerUtil.getActiveWorkbenchWindow(event)
				.getActivePage().getSelection();

		if (selection != null && !selection.isEmpty()
				&& selection instanceof IStructuredSelection) {

			long size = 0;

			Iterator<?> it = ((IStructuredSelection) selection).iterator();

			// TODO enhance this: as the size method is recursive, we keep track
			// of nodes for which we already have computed size so that we don't
			// count them twice. In a first approximation, we assume that the
			// structure selection keep the nodes ordered.
			List<String> importedPathes = new ArrayList<String>();
			try {
				nodesIt: while (it.hasNext()) {
					Object obj = it.next();
					String curPath;
					Node node;
					if (obj instanceof SingleJcrNodeElem) {
						node = ((SingleJcrNodeElem) obj).getNode();
						curPath = node.getSession().getWorkspace().getName();
						curPath += "/" + node.getPath();
					} else if (obj instanceof WorkspaceElem) {
						node = ((WorkspaceElem) obj).getRootNode();
						curPath = node.getSession().getWorkspace().getName();
					} else
						// non valid object type
						continue nodesIt;

					Iterator<String> itPath = importedPathes.iterator();
					while (itPath.hasNext()) {
						String refPath = itPath.next();
						if (curPath.startsWith(refPath))
							// Already done : skip node
							continue nodesIt;
					}
					size += JcrUtils.getNodeApproxSize(node);
					importedPathes.add(curPath);
				}
			} catch (Exception e) {
				ErrorFeedback.show("Cannot Get size of selected node ", e);
			}

			String[] labels = { "OK" };
			Shell shell = HandlerUtil.getActiveWorkbenchWindow(event)
					.getShell();
			MessageDialog md = new MessageDialog(shell, "Node size", null,
					"Node size is: " + size / 1024 + " KB",
					MessageDialog.INFORMATION, labels, 0);
			md.open();
		}
		return null;
	}
}
