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

import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.jcr.Binary;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.nodetype.NodeType;

import org.argeo.cms.ui.jcr.model.SingleJcrNodeElem;
import org.argeo.cms.ui.jcr.model.WorkspaceElem;
import org.argeo.cms.ui.workbench.WorkbenchUiPlugin;
import org.argeo.cms.ui.workbench.jcr.JcrBrowserView;
import org.argeo.eclipse.ui.EclipseUiUtils;
import org.argeo.eclipse.ui.TreeParent;
import org.argeo.eclipse.ui.dialogs.ErrorFeedback;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.ui.handlers.HandlerUtil;

/** Upload local file(s) under the currently selected node */
public class UploadFiles extends AbstractHandler {
	// private final static Log log = LogFactory.getLog(ImportFileSystem.class);

	public Object execute(ExecutionEvent event) throws ExecutionException {

		ISelection selection = HandlerUtil.getActiveWorkbenchWindow(event).getActivePage().getSelection();
		JcrBrowserView view = (JcrBrowserView) HandlerUtil.getActiveWorkbenchWindow(event).getActivePage()
				.findView(HandlerUtil.getActivePartId(event));
		if (selection != null && !selection.isEmpty() && selection instanceof IStructuredSelection) {
			Object obj = ((IStructuredSelection) selection).getFirstElement();
			try {
				Node folder = null;
				if (obj instanceof SingleJcrNodeElem) {
					folder = ((SingleJcrNodeElem) obj).getNode();
				} else if (obj instanceof WorkspaceElem) {
					folder = ((WorkspaceElem) obj).getRootNode();
				} else {
					ErrorFeedback.show(WorkbenchUiPlugin.getMessage("warningInvalidNodeToImport"));
				}
				if (folder != null) {
					FileDialog dialog = new FileDialog(HandlerUtil.getActiveShell(event), SWT.MULTI);
					dialog.setText("Choose one or more files to upload");

					if (EclipseUiUtils.notEmpty(dialog.open())) {
						String[] names = dialog.getFileNames();
						// Workaround small differences between RAP and RCP
						// 1. returned names are absolute path on RAP and
						// relative in RCP
						// 2. in RCP we must use getFilterPath that does not
						// exists on RAP
						Method filterMethod = null;
						Path parPath = null;

						try {
							filterMethod = dialog.getClass().getDeclaredMethod("getFilterPath");
							String filterPath = (String) filterMethod.invoke(dialog);
							parPath = Paths.get(filterPath);
						} catch (NoSuchMethodException nsme) { // RAP
						}
						if (names.length == 0)
							return null;
						else {
							loop: for (String name : names) {
								Path path = Paths.get(name);
								if (parPath != null)
									path = parPath.resolve(path);
								if (Files.exists(path)) {
									URI uri = path.toUri();
									String uriStr = uri.toString();
									System.out.println(uriStr);

									if (Files.isDirectory(path)) {
										MessageDialog.openError(HandlerUtil.getActiveShell(event),
												"Unimplemented directory import",
												"Upload of directories in the system is not yet implemented");
										continue loop;
									}
									Node fileNode = folder.addNode(path.getFileName().toString(), NodeType.NT_FILE);
									Node resNode = fileNode.addNode(Property.JCR_CONTENT, NodeType.NT_RESOURCE);
									Binary binary = null;
									try (InputStream is = Files.newInputStream(path)) {
										binary = folder.getSession().getValueFactory().createBinary(is);
										resNode.setProperty(Property.JCR_DATA, binary);
									}
									folder.getSession().save();
								} else {
									String msg = "Cannot upload file at " + path.toString();
									if (parPath != null)
										msg += "\nPlease remember that file upload fails when choosing files from the \"Recently Used\" bookmarks on some OS";
									MessageDialog.openError(HandlerUtil.getActiveShell(event), "Missing file", msg);
									continue loop;
								}
							}
							view.nodeAdded((TreeParent) obj);
							return true;
						}
					}
				}
			} catch (Exception e) {
				ErrorFeedback.show("Cannot import files to " + obj, e);
			}
		}
		return null;
	}
}
