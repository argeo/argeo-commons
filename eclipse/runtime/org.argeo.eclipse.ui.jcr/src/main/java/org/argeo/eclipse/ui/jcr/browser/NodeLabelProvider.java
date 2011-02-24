package org.argeo.eclipse.ui.jcr.browser;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeType;

import org.argeo.ArgeoException;
import org.argeo.eclipse.ui.jcr.JcrUiPlugin;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;

public class NodeLabelProvider extends LabelProvider {
	// Images
	public final static Image NODE = JcrUiPlugin.getImageDescriptor(
			"icons/node.gif").createImage();
	public final static Image FOLDER = JcrUiPlugin.getImageDescriptor(
			"icons/folder.gif").createImage();
	public final static Image FILE = JcrUiPlugin.getImageDescriptor(
			"icons/file.gif").createImage();
	public final static Image BINARY = JcrUiPlugin.getImageDescriptor(
			"icons/binary.png").createImage();

	public String getText(Object element) {
		try {
			if (element instanceof Node) {
				Node node = (Node) element;
				String label = node.getName();
				// try {
				// Item primaryItem = node.getPrimaryItem();
				// label = primaryItem instanceof Property ? ((Property)
				// primaryItem)
				// .getValue().getString()
				// + " ("
				// + node.getName()
				// + ")" : node.getName();
				// } catch (RepositoryException e) {
				// label = node.getName();
				// }
				StringBuffer mixins = new StringBuffer("");
				for (NodeType type : node.getMixinNodeTypes())
					mixins.append(' ').append(type.getName());

				return label + " [" + node.getPrimaryNodeType().getName()
						+ mixins + "]";
			}
			return element.toString();
		} catch (RepositoryException e) {
			throw new ArgeoException("Cannot get text for of " + element, e);
		}
	}

	@Override
	public Image getImage(Object element) {
		if (element instanceof Node) {
			Node node = (Node) element;
			try {
				if (node.getPrimaryNodeType().isNodeType(NodeType.NT_FOLDER))
					return FOLDER;
				else if (node.getPrimaryNodeType().isNodeType(NodeType.NT_FILE))
					return FILE;
				else if (node.getPrimaryNodeType().isNodeType(
						NodeType.NT_RESOURCE))
					return BINARY;
			} catch (RepositoryException e) {
				// silent
			}
			return NODE;
		} else if (element instanceof RepositoryNode) {
			if (((RepositoryNode) element).getDefaultSession() == null)
				return RepositoryNode.REPOSITORY_DISCONNECTED;
			else
				return RepositoryNode.REPOSITORY_CONNECTED;
		} else if (element instanceof WorkspaceNode) {
			if (((WorkspaceNode) element).getSession() == null)
				return WorkspaceNode.WORKSPACE_DISCONNECTED;
			else
				return WorkspaceNode.WORKSPACE_CONNECTED;
		}
		return super.getImage(element);
	}
}
