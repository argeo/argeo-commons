package org.argeo.jcr.ui.explorer.browser;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeType;

import org.argeo.ArgeoException;
import org.argeo.eclipse.ui.jcr.DefaultNodeLabelProvider;
import org.argeo.eclipse.ui.jcr.JcrUiPlugin;
import org.argeo.jcr.ui.explorer.model.RepositoriesNode;
import org.argeo.jcr.ui.explorer.model.RepositoryNode;
import org.argeo.jcr.ui.explorer.model.SingleJcrNode;
import org.argeo.jcr.ui.explorer.model.WorkspaceNode;
import org.eclipse.swt.graphics.Image;

public class NodeLabelProvider extends DefaultNodeLabelProvider {
	// Images
	public final static Image REPOSITORIES = JcrUiPlugin.getImageDescriptor(
			"icons/repositories.gif").createImage();

	public String getText(Object element) {
		try {
			if (element instanceof SingleJcrNode) {
				SingleJcrNode sjn = (SingleJcrNode) element;
				return getText(sjn.getNode());
			} else
				return super.getText(element);
		} catch (RepositoryException e) {
			throw new ArgeoException(
					"Unexpected JCR error while getting node name.");
		}
	}

	protected String getText(Node node) throws RepositoryException {
		String label = node.getName();
		StringBuffer mixins = new StringBuffer("");
		for (NodeType type : node.getMixinNodeTypes())
			mixins.append(' ').append(type.getName());

		return label + " [" + node.getPrimaryNodeType().getName() + mixins
				+ "]";
	}

	@Override
	public Image getImage(Object element) {
		if (element instanceof RepositoryNode) {
			if (((RepositoryNode) element).getDefaultSession() == null)
				return RepositoryNode.REPOSITORY_DISCONNECTED;
			else
				return RepositoryNode.REPOSITORY_CONNECTED;
		} else if (element instanceof WorkspaceNode) {
			if (((WorkspaceNode) element).getSession() == null)
				return WorkspaceNode.WORKSPACE_DISCONNECTED;
			else
				return WorkspaceNode.WORKSPACE_CONNECTED;
		} else if (element instanceof RepositoriesNode) {
			return REPOSITORIES;
		} else if (element instanceof SingleJcrNode)
			try {
				return super.getImage(((SingleJcrNode) element).getNode());
			} catch (RepositoryException e) {
				return null;
			}
		return super.getImage(element);
	}

}
