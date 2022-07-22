package org.argeo.eclipse.ui.jcr;

import javax.jcr.RepositoryException;
import javax.jcr.query.Row;

import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;

/** Simplifies writing JCR-based column label provider. */
public class RowColumnLabelProvider extends ColumnLabelProvider {
	private static final long serialVersionUID = -6586692836928505358L;

	protected String getRowText(Row row) throws RepositoryException {
		return super.getText(row);
	}

	protected String getRowToolTipText(Row row) throws RepositoryException {
		return super.getToolTipText(row);
	}

	protected Image getRowImage(Row row) throws RepositoryException {
		return super.getImage(row);
	}

	protected Font getRowFont(Row row) throws RepositoryException {
		return super.getFont(row);
	}

	public Color getRowBackground(Row row) throws RepositoryException {
		return super.getBackground(row);
	}

	public Color getRowForeground(Row row) throws RepositoryException {
		return super.getForeground(row);
	}

	@Override
	public String getText(Object element) {
		try {
			if (element instanceof Row)
				return getRowText((Row) element);
			else
				throw new IllegalArgumentException("Unsupported element type " + element.getClass());
		} catch (RepositoryException e) {
			throw new IllegalStateException("Repository exception when accessing " + element, e);
		}
	}

	@Override
	public Image getImage(Object element) {
		try {
			if (element instanceof Row)
				return getRowImage((Row) element);
			else
				throw new IllegalArgumentException("Unsupported element type " + element.getClass());
		} catch (RepositoryException e) {
			throw new IllegalStateException("Repository exception when accessing " + element, e);
		}
	}

	@Override
	public String getToolTipText(Object element) {
		try {
			if (element instanceof Row)
				return getRowToolTipText((Row) element);
			else
				throw new IllegalArgumentException("Unsupported element type " + element.getClass());
		} catch (RepositoryException e) {
			throw new IllegalStateException("Repository exception when accessing " + element, e);
		}
	}

	@Override
	public Font getFont(Object element) {
		try {
			if (element instanceof Row)
				return getRowFont((Row) element);
			else
				throw new IllegalArgumentException("Unsupported element type " + element.getClass());
		} catch (RepositoryException e) {
			throw new IllegalStateException("Repository exception when accessing " + element, e);
		}
	}

	@Override
	public Color getBackground(Object element) {
		try {
			if (element instanceof Row)
				return getRowBackground((Row) element);
			else
				throw new IllegalArgumentException("Unsupported element type " + element.getClass());
		} catch (RepositoryException e) {
			throw new IllegalStateException("Repository exception when accessing " + element, e);
		}
	}

	@Override
	public Color getForeground(Object element) {
		try {
			if (element instanceof Row)
				return getRowForeground((Row) element);
			else
				throw new IllegalArgumentException("Unsupported element type " + element.getClass());
		} catch (RepositoryException e) {
			throw new IllegalStateException("Repository exception when accessing " + element, e);
		}
	}

}
