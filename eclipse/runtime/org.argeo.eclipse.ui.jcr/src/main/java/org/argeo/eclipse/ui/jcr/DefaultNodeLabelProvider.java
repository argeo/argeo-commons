package org.argeo.eclipse.ui.jcr;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeType;

import org.argeo.ArgeoException;
import org.argeo.jcr.ArgeoTypes;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.swt.graphics.Image;

/** Provides reasonable overridable defaults for know JCR types. */
public class DefaultNodeLabelProvider extends ColumnLabelProvider {
	// Images
	/**
	 * @deprecated Use {@link JcrImages#NODE} instead
	 */
	public final static Image NODE = JcrImages.NODE;
	/**
	 * @deprecated Use {@link JcrImages#FOLDER} instead
	 */
	public final static Image FOLDER = JcrImages.FOLDER;
	/**
	 * @deprecated Use {@link JcrImages#FILE} instead
	 */
	public final static Image FILE = JcrImages.FILE;
	/**
	 * @deprecated Use {@link JcrImages#BINARY} instead
	 */
	public final static Image BINARY = JcrImages.BINARY;
	/**
	 * @deprecated Use {@link JcrImages#HOME} instead
	 */
	public final static Image HOME = JcrImages.HOME;

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
			throw new ArgeoException("Cannot get text for of " + element, e);
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
			throw new ArgeoException("Cannot retrieve image for " + element, e);
		}
		return super.getImage(element);
	}

	protected Image getImage(Node node) throws RepositoryException {
		// optimized order
		if (node.getPrimaryNodeType().isNodeType(NodeType.NT_FILE))
			return JcrImages.FILE;
		else if (node.getPrimaryNodeType().isNodeType(NodeType.NT_FOLDER))
			return JcrImages.FOLDER;
		else if (node.getPrimaryNodeType().isNodeType(NodeType.NT_RESOURCE))
			return JcrImages.BINARY;
		else if (node.isNodeType(ArgeoTypes.ARGEO_USER_HOME))
			return JcrImages.HOME;
		else
			return JcrImages.NODE;
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
			throw new ArgeoException("Cannot get tooltip for " + element, e);
		}
		return super.getToolTipText(element);
	}

	protected String getToolTipText(Node node) throws RepositoryException {
		return null;
	}

}
