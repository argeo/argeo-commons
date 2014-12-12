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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.argeo.ArgeoException;
import org.argeo.eclipse.ui.specific.OpenFile;
import org.argeo.eclipse.ui.workbench.CommandUtils;
import org.argeo.eclipse.ui.workbench.WorkbenchUiPlugin;
import org.argeo.jcr.JcrUtils;
import org.argeo.eclipse.ui.workbench.jcr.internal.model.SingleJcrNodeElem;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.handlers.HandlerUtil;

/**
 * If the method
 * <code> HandlerUtil.getActiveWorkbenchWindow(event).getActivePage().getSelection() </code>
 * exits and has a SingleJcrNodeElem as first element, it canonically calls the
 * JCR Session.exportSystemView() method on the underlying node with both
 * skipBinary & noRecurse boolean flags set to false.
 * 
 * Resulting stream is saved in a tmp file and opened via the "open file"
 * single-sourced command.
 */
public class DumpNode extends AbstractHandler {
	public final static String ID = WorkbenchUiPlugin.ID + ".dumpNode";

	private final static DateFormat df = new SimpleDateFormat(
			"yyyy-MM-dd_HH-mm");

	public Object execute(ExecutionEvent event) throws ExecutionException {
		ISelection selection = HandlerUtil.getActiveWorkbenchWindow(event)
				.getActivePage().getSelection();
		if (selection == null || !(selection instanceof IStructuredSelection))
			return null;

		Iterator<?> lst = ((IStructuredSelection) selection).iterator();
		if (lst.hasNext()) {
			Object element = lst.next();
			if (element instanceof SingleJcrNodeElem) {
				SingleJcrNodeElem sjn = (SingleJcrNodeElem) element;
				Node node = sjn.getNode();

				// TODO add a dialog to configure the export and ask for
				// confirmation
				// Boolean ok = MessageDialog.openConfirm(
				// HandlerUtil.getActiveShell(event), "Confirm deletion",
				// "Do you want to delete " + buf + "?");

				File tmpFile;
				FileOutputStream fos;
				try {
					tmpFile = File.createTempFile("JcrExport", ".xml");
					tmpFile.deleteOnExit();
					fos = new FileOutputStream(tmpFile);
					String dateVal = df.format(new GregorianCalendar()
							.getTime());
					node.getSession().exportSystemView(node.getPath(), fos,
							true, false);
					openGeneratedFile(tmpFile.getAbsolutePath(),
							"Dump-" + JcrUtils.replaceInvalidChars(node.getName())+ "-" + dateVal + ".xml");
				} catch (RepositoryException e) {
					throw new ArgeoException(
							"Unable to perform SystemExport on " + node, e);
				} catch (IOException e) {
					throw new ArgeoException("Unable to SystemExport " + node,
							e);
				}
			}
		}
		return null;
	}

	private synchronized void openGeneratedFile(String path, String fileName) {
		Map<String, String> params = new HashMap<String, String>();
		params.put(OpenFile.PARAM_FILE_NAME, fileName);
		params.put(OpenFile.PARAM_FILE_URI, "file://" + path);
		CommandUtils.callCommand("org.argeo.security.ui.specific.openFile",
				params);
	}
}
