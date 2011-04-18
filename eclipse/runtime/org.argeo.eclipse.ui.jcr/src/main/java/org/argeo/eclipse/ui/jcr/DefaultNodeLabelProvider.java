package org.argeo.eclipse.ui.jcr;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeType;

import org.argeo.ArgeoException;
import org.argeo.jcr.ArgeoTypes;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;

/** Provides reasonable overridable defaults for know JCR types. */
public class DefaultNodeLabelProvider extends LabelProvider {
	// Images
	public final static Image NODE = JcrUiPlugin.getImageDescriptor(
			"icons/node.gif").createImage();
	public final static Image FOLDER = JcrUiPlugin.getImageDescriptor(
			"icons/folder.gif").createImage();
	public final static Image FILE = JcrUiPlugin.getImageDescriptor(
			"icons/file.gif").createImage();
	public final static Image BINARY = JcrUiPlugin.getImageDescriptor(
			"icons/binary.png").createImage();
	public final static Image HOME = JcrUiPlugin.getImageDescriptor(
			"icons/home.gif").createImage();

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
			return FILE;
		else if (node.getPrimaryNodeType().isNodeType(NodeType.NT_FOLDER))
			return FOLDER;
		else if (node.getPrimaryNodeType().isNodeType(NodeType.NT_RESOURCE))
			return BINARY;
		else if (node.isNodeType(ArgeoTypes.ARGEO_USER_HOME))
			return HOME;
		else
			return NODE;
	}

}
