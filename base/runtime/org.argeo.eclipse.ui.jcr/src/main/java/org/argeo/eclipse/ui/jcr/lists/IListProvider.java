package org.argeo.eclipse.ui.jcr.lists;

import java.util.List;

/**
 * Views and editors can implement this interface so that one of the row list
 * that is displayed in the part (For instance in a Table or a Tree Viewer) can
 * be rebuilt externally. typically to generate csv or calc extract.
 */
public interface IListProvider {
	/**
	 * Returns an array of current and relevant elements
	 */
	public Object[] getElements(String extractId);

	/**
	 * Returns the column definition for passed ID
	 */
	public List<ColumnDefinition> getColumnDefinition(String extractId);
}
