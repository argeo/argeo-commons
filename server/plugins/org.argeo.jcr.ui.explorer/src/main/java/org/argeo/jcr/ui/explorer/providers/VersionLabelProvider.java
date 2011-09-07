package org.argeo.jcr.ui.explorer.providers;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.version.Version;

import org.argeo.ArgeoException;
import org.eclipse.jface.viewers.ColumnLabelProvider;

/**
 * simple wrapping of the ColumnLabelProvider class to provide text display in
 * order to build a tree for version. The Get text method does not assume that
 * Version extends Node class to respect JCR 2.0 specification
 * 
 */
public class VersionLabelProvider extends ColumnLabelProvider {

	public String getText(Object element) {
		try {
			if (element instanceof Version) {
				Version version = (Version) element;
				return version.getName();
			} else if (element instanceof Node) {
				return ((Node) element).getName();
			}
		} catch (RepositoryException re) {
			throw new ArgeoException(
					"Unexpected error while getting element name", re);
		}
		return super.getText(element);
	}
}
