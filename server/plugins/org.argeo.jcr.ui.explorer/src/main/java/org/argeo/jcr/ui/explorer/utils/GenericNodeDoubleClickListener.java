package org.argeo.jcr.ui.explorer.utils;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeType;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.argeo.ArgeoException;
import org.argeo.eclipse.ui.specific.FileHandler;
import org.argeo.jcr.ui.explorer.JcrExplorerPlugin;
import org.argeo.jcr.ui.explorer.browser.NodeContentProvider;
import org.argeo.jcr.ui.explorer.browser.RepositoryNode;
import org.argeo.jcr.ui.explorer.browser.WorkspaceNode;
import org.argeo.jcr.ui.explorer.editors.GenericNodeEditor;
import org.argeo.jcr.ui.explorer.editors.GenericNodeEditorInput;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.ui.PartInitException;

/**
 * 
 * Centralizes the management of double click on a NodeTreeViewer
 * 
 */
public class GenericNodeDoubleClickListener implements IDoubleClickListener {

	private final static Log log = LogFactory
			.getLog(GenericNodeDoubleClickListener.class);

	private TreeViewer nodeViewer;
	private JcrFileProvider jfp;
	private FileHandler fileHandler;

	public GenericNodeDoubleClickListener(TreeViewer nodeViewer) {
		this.nodeViewer = nodeViewer;
		jfp = new JcrFileProvider();
		fileHandler = new FileHandler(jfp);
	}

	public void doubleClick(DoubleClickEvent event) {
		if (event.getSelection() == null || event.getSelection().isEmpty())
			return;
		Object obj = ((IStructuredSelection) event.getSelection())
				.getFirstElement();
		if (obj instanceof RepositoryNode) {
			RepositoryNode rpNode = (RepositoryNode) obj;
			rpNode.login();
			nodeViewer.refresh(obj);
		} else if (obj instanceof WorkspaceNode) {
			((WorkspaceNode) obj).login();
			nodeViewer.refresh(obj);
		} else if (obj instanceof Node) {
			Node node = (Node) obj;
			try {
				if (node.isNodeType(NodeType.NT_FILE)) {
					// double click on a file node triggers its opening
					String name = node.getName();
					String id = node.getIdentifier();

					// For the file provider to be able to browse the
					// various
					// repository.
					// TODO : enhanced that.
					ITreeContentProvider itcp = (ITreeContentProvider) nodeViewer
							.getContentProvider();
					jfp.setRootNodes((Object[]) itcp.getElements(null));
					fileHandler.openFile(name, id);
				}
				GenericNodeEditorInput gnei = new GenericNodeEditorInput(node);
				JcrExplorerPlugin.getDefault().getWorkbench()
						.getActiveWorkbenchWindow().getActivePage()
						.openEditor(gnei, GenericNodeEditor.ID);
			} catch (RepositoryException re) {
				throw new ArgeoException(
						"Repository error while getting node info", re);
			} catch (PartInitException pie) {
				throw new ArgeoException(
						"Unexepected exception while opening node editor", pie);
			}
		}
	}

	// Enhance this method
	private String getRepositoryAlias(Object element) {
		NodeContentProvider ncp = (NodeContentProvider) nodeViewer
				.getContentProvider();
		Object parent = element;
		while (!(ncp.getParent(parent) instanceof RepositoryNode)
				&& parent != null)
			parent = ncp.getParent(parent);
		return parent == null ? null : ((RepositoryNode) parent).getName();
	}
}
