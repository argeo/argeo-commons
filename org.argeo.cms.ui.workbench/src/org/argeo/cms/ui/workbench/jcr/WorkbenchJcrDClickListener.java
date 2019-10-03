package org.argeo.cms.ui.workbench.jcr;

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
import org.argeo.cms.ui.jcr.JcrDClickListener;
import org.argeo.cms.ui.workbench.WorkbenchUiPlugin;
import org.argeo.cms.ui.workbench.internal.jcr.parts.GenericNodeEditorInput;
import org.argeo.cms.ui.workbench.util.CommandUtils;
import org.argeo.eclipse.ui.EclipseUiException;
import org.argeo.eclipse.ui.specific.OpenFile;
import org.argeo.eclipse.ui.specific.SingleSourcingException;
import org.argeo.jcr.JcrUtils;
import org.eclipse.jface.viewers.TreeViewer;

public class WorkbenchJcrDClickListener extends JcrDClickListener {

	public WorkbenchJcrDClickListener(TreeViewer nodeViewer) {
		super(nodeViewer);
	}

	@Override
	protected void openNode(Node node) {
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
			WorkbenchUiPlugin.getDefault().getWorkbench().getActiveWorkbenchWindow().getActivePage().openEditor(gnei,
					DefaultNodeEditor.ID);
		} catch (RepositoryException re) {
			throw new EclipseUiException("Repository error while getting node info", re);
		} catch (Exception pie) {
			throw new EclipseUiException("Unexpected exception while opening node editor", pie);
		}
	}
}
