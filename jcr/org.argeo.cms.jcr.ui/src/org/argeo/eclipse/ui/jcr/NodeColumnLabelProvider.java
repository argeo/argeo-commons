package org.argeo.eclipse.ui.jcr;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;

/** Simplifies writing JCR-based column label provider. */
public class NodeColumnLabelProvider extends ColumnLabelProvider {
	private static final long serialVersionUID = -6586692836928505358L;

	protected String getNodeText(Node node) throws RepositoryException {
		return super.getText(node);
	}

	protected String getNodeToolTipText(Node node) throws RepositoryException {
		return super.getToolTipText(node);
	}

	protected Image getNodeImage(Node node) throws RepositoryException {
		return super.getImage(node);
	}

	protected Font getNodeFont(Node node) throws RepositoryException {
		return super.getFont(node);
	}

	public Color getNodeBackground(Node node) throws RepositoryException {
		return super.getBackground(node);
	}

	public Color getNodeForeground(Node node) throws RepositoryException {
		return super.getForeground(node);
	}

	@Override
	public String getText(Object element) {
		try {
			if (element instanceof Node)
				return getNodeText((Node) element);
			else if (element instanceof NodeElement)
				return getNodeText(((NodeElement) element).getNode());
			else
				throw new IllegalArgumentException("Unsupported element type " + element.getClass());
		} catch (RepositoryException e) {
			throw new IllegalStateException("Repository exception when accessing " + element, e);
		}
	}

	@Override
	public Image getImage(Object element) {
		try {
			if (element instanceof Node)
				return getNodeImage((Node) element);
			else if (element instanceof NodeElement)
				return getNodeImage(((NodeElement) element).getNode());
			else
				throw new IllegalArgumentException("Unsupported element type " + element.getClass());
		} catch (RepositoryException e) {
			throw new IllegalStateException("Repository exception when accessing " + element, e);
		}
	}

	@Override
	public String getToolTipText(Object element) {
		try {
			if (element instanceof Node)
				return getNodeToolTipText((Node) element);
			else if (element instanceof NodeElement)
				return getNodeToolTipText(((NodeElement) element).getNode());
			else
				throw new IllegalArgumentException("Unsupported element type " + element.getClass());
		} catch (RepositoryException e) {
			throw new IllegalStateException("Repository exception when accessing " + element, e);
		}
	}

	@Override
	public Font getFont(Object element) {
		try {
			if (element instanceof Node)
				return getNodeFont((Node) element);
			else if (element instanceof NodeElement)
				return getNodeFont(((NodeElement) element).getNode());
			else
				throw new IllegalArgumentException("Unsupported element type " + element.getClass());
		} catch (RepositoryException e) {
			throw new IllegalStateException("Repository exception when accessing " + element, e);
		}
	}

	@Override
	public Color getBackground(Object element) {
		try {
			if (element instanceof Node)
				return getNodeBackground((Node) element);
			else if (element instanceof NodeElement)
				return getNodeBackground(((NodeElement) element).getNode());
			else
				throw new IllegalArgumentException("Unsupported element type " + element.getClass());
		} catch (RepositoryException e) {
			throw new IllegalStateException("Repository exception when accessing " + element, e);
		}
	}

	@Override
	public Color getForeground(Object element) {
		try {
			if (element instanceof Node)
				return getNodeForeground((Node) element);
			else if (element instanceof NodeElement)
				return getNodeForeground(((NodeElement) element).getNode());
			else
				throw new IllegalArgumentException("Unsupported element type " + element.getClass());
		} catch (RepositoryException e) {
			throw new IllegalStateException("Repository exception when accessing " + element, e);
		}
	}

}
