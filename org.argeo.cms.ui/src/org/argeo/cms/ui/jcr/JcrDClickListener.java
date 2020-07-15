package org.argeo.cms.ui.jcr;

import javax.jcr.Node;

import org.argeo.cms.ui.jcr.model.RepositoryElem;
import org.argeo.cms.ui.jcr.model.SingleJcrNodeElem;
import org.argeo.cms.ui.jcr.model.WorkspaceElem;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;

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
			openNode(node);
		}
	}

	protected void openNode(Node node) {
		// TODO implement generic behaviour
	}
}
