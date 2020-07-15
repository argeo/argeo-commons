package org.argeo.cms.ui.jcr;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.version.Version;

import org.argeo.eclipse.ui.EclipseUiException;
import org.eclipse.jface.viewers.ColumnLabelProvider;

/**
 * Simple wrapping of the ColumnLabelProvider class to provide text display in
 * order to build a tree for version. The getText() method does not assume that
 * {@link Version} extends {@link Node} class to respect JCR 2.0 specification
 * 
 */
public class VersionLabelProvider extends ColumnLabelProvider {
	private static final long serialVersionUID = 5270739851193688238L;

	public String getText(Object element) {
		try {
			if (element instanceof Version) {
				Version version = (Version) element;
				return version.getName();
			} else if (element instanceof Node) {
				return ((Node) element).getName();
			}
		} catch (RepositoryException re) {
			throw new EclipseUiException(
					"Unexpected error while getting element name", re);
		}
		return super.getText(element);
	}
}
