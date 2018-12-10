package org.argeo.eclipse.ui.jcr;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.eclipse.jface.viewers.ColumnLabelProvider;
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

	@Override
	public String getText(Object element) {
		try {
			return getNodeText((Node) element);
		} catch (RepositoryException e) {
			throw new RuntimeException("Runtime repository exception when accessing " + element, e);
		}
	}

	@Override
	public Image getImage(Object element) {
		try {
			return getNodeImage((Node) element);
		} catch (RepositoryException e) {
			throw new RuntimeException("Runtime repository exception when accessing " + element, e);
		}
	}

	@Override
	public String getToolTipText(Object element) {
		try {
			return getNodeToolTipText((Node) element);
		} catch (RepositoryException e) {
			throw new RuntimeException("Runtime repository exception when accessing " + element, e);
		}
	}

}
