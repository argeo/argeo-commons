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
package org.argeo.cms.ui.workbench.internal.jcr;

import static javax.jcr.Node.JCR_CONTENT;
import static javax.jcr.Property.JCR_DATA;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import javax.jcr.Binary;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeType;

import org.apache.commons.io.IOUtils;
import org.argeo.cms.ui.workbench.WorkbenchUiPlugin;
import org.argeo.cms.ui.workbench.internal.jcr.model.RepositoryElem;
import org.argeo.cms.ui.workbench.internal.jcr.model.SingleJcrNodeElem;
import org.argeo.cms.ui.workbench.internal.jcr.model.WorkspaceElem;
import org.argeo.cms.ui.workbench.internal.jcr.parts.GenericNodeEditorInput;
import org.argeo.cms.ui.workbench.jcr.DefaultNodeEditor;
import org.argeo.cms.ui.workbench.util.CommandUtils;
import org.argeo.eclipse.ui.EclipseUiException;
import org.argeo.eclipse.ui.specific.OpenFile;
import org.argeo.eclipse.ui.specific.SingleSourcingException;
import org.argeo.jcr.JcrUtils;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.ui.PartInitException;

/** Centralizes the management of double click on a NodeTreeViewer */
public class JcrDClickListener implements IDoubleClickListener {
	// private final static Log log = LogFactory
	// .getLog(GenericNodeDoubleClickListener.class);

	private TreeViewer nodeViewer;

	// private JcrFileProvider jfp;
	// private FileHandler fileHandler;

	public JcrDClickListener(TreeViewer nodeViewer) {
		this.nodeViewer = nodeViewer;
		// jfp = new JcrFileProvider();
		// Commented out. see https://www.argeo.org/bugzilla/show_bug.cgi?id=188
		// fileHandler = null;
		// fileHandler = new FileHandler(jfp);
	}

	public void doubleClick(DoubleClickEvent event) {
		if (event.getSelection() == null || event.getSelection().isEmpty())
			return;
		Object obj = ((IStructuredSelection) event.getSelection()).getFirstElement();
		if (obj instanceof RepositoryElem) {
			RepositoryElem rpNode = (RepositoryElem) obj;
			if (rpNode.isConnected()) {
				rpNode.logout();
			} else {
				rpNode.login();
			}
			nodeViewer.refresh(obj);
		} else if (obj instanceof WorkspaceElem) {
			WorkspaceElem wn = (WorkspaceElem) obj;
			if (wn.isConnected())
				wn.logout();
			else
				wn.login();
			nodeViewer.refresh(obj);
		} else if (obj instanceof SingleJcrNodeElem) {
			SingleJcrNodeElem sjn = (SingleJcrNodeElem) obj;
			Node node = sjn.getNode();
			try {
				if (node.isNodeType(NodeType.NT_FILE)) {
					// Also open it

					String name = node.getName();
					Map<String, String> params = new HashMap<String, String>();
					params.put(OpenFile.PARAM_FILE_NAME, name);

					// TODO rather directly transmit the path to the node, once
					// we have defined convention to provide an Absolute URI to
					// a node in a multi repo / workspace / user context
					// params.put(OpenFile.PARAM_FILE_URI,
					// OpenFileService.JCR_SCHEME + node.getPath());

					// we copy the node to a tmp file to be opened as a dirty
					// workaround
					File tmpFile = null;
					// OutputStream os = null;
					// InputStream is = null;
					int i = name.lastIndexOf('.');
					String prefix, suffix;
					if (i == -1) {
						prefix = name;
						suffix = null;
					} else {
						prefix = name.substring(0, i);
						suffix = name.substring(i);
					}
					Binary binary = null;
					try {
						tmpFile = File.createTempFile(prefix, suffix);
						tmpFile.deleteOnExit();
					} catch (IOException e1) {
						throw new EclipseUiException("Cannot create temp file", e1);
					}
					try (OutputStream os = new FileOutputStream(tmpFile)) {
						binary = node.getNode(JCR_CONTENT).getProperty(JCR_DATA).getBinary();
						try (InputStream is = binary.getStream();) {
							IOUtils.copy(is, os);
						}
					} catch (IOException e) {
						throw new SingleSourcingException("Cannot open file " + prefix + "." + suffix, e);
					} finally {
						// IOUtils.closeQuietly(is);
						// IOUtils.closeQuietly(os);
						JcrUtils.closeQuietly(binary);
					}
					Path path = Paths.get(tmpFile.getAbsolutePath());
					String uri = path.toUri().toString();
					params.put(OpenFile.PARAM_FILE_URI, uri);
					CommandUtils.callCommand(OpenFile.ID, params);
				}
				GenericNodeEditorInput gnei = new GenericNodeEditorInput(node);
				WorkbenchUiPlugin.getDefault().getWorkbench().getActiveWorkbenchWindow().getActivePage()
						.openEditor(gnei, DefaultNodeEditor.ID);
			} catch (RepositoryException re) {
				throw new EclipseUiException("Repository error while getting node info", re);
			} catch (PartInitException pie) {
				throw new EclipseUiException("Unexepected exception while opening node editor", pie);
			}
		}
	}
}
