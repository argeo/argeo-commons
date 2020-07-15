package org.argeo.eclipse.ui.jcr;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeType;

import org.argeo.eclipse.ui.EclipseUiException;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.swt.graphics.Image;

/**
 * Default label provider to manage node and corresponding UI objects. It
 * provides reasonable overwrite-able default for known JCR types.
 */
public class DefaultNodeLabelProvider extends ColumnLabelProvider {
	private static final long serialVersionUID = 1216182332792151235L;

	public String getText(Object element) {
		try {
			if (element instanceof Node) {
				return getText((Node) element);
			} else if (element instanceof WrappedNode) {
				return getText(((WrappedNode) element).getNode());
			} else if (element instanceof NodesWrapper) {
				return getText(((NodesWrapper) element).getNode());
			}
			return super.getText(element);
		} catch (RepositoryException e) {
			throw new EclipseUiException("Cannot get text for of " + element, e);
		}
	}

	protected String getText(Node node) throws RepositoryException {
		if (node.isNodeType(NodeType.MIX_TITLE)
				&& node.hasProperty(Property.JCR_TITLE))
			return node.getProperty(Property.JCR_TITLE).getString();
		else
			return node.getName();
	}

	@Override
	public Image getImage(Object element) {
		try {
			if (element instanceof Node) {
				return getImage((Node) element);
			} else if (element instanceof WrappedNode) {
				return getImage(((WrappedNode) element).getNode());
			} else if (element instanceof NodesWrapper) {
				return getImage(((NodesWrapper) element).getNode());
			}
		} catch (RepositoryException e) {
			throw new EclipseUiException("Cannot retrieve image for " + element, e);
		}
		return super.getImage(element);
	}

	protected Image getImage(Node node) throws RepositoryException {
		// FIXME who uses that?
		return null;
	}

	@Override
	public String getToolTipText(Object element) {
		try {
			if (element instanceof Node) {
				return getToolTipText((Node) element);
			} else if (element instanceof WrappedNode) {
				return getToolTipText(((WrappedNode) element).getNode());
			} else if (element instanceof NodesWrapper) {
				return getToolTipText(((NodesWrapper) element).getNode());
			}
		} catch (RepositoryException e) {
			throw new EclipseUiException("Cannot get tooltip for " + element, e);
		}
		return super.getToolTipText(element);
	}

	protected String getToolTipText(Node node) throws RepositoryException {
		return null;
	}
}