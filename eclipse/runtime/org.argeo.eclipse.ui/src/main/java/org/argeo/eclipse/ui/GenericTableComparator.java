package org.argeo.eclipse.ui;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;

public abstract class GenericTableComparator extends ViewerComparator {

	protected int propertyIndex;
	public static final int ASCENDING = 0, DESCENDING = 1;
	protected int direction = DESCENDING;

	/**
	 * Creates an instance of a sorter for TableViewer.
	 * 
	 * @param defaultColumn
	 *            the default sorter column
	 */

	public GenericTableComparator(int defaultColumnIndex, int direction) {
		propertyIndex = defaultColumnIndex;
		this.direction = direction;
	}

	public void setColumn(int column) {
		if (column == this.propertyIndex) {
			// Same column as last sort; toggle the direction
			direction = 1 - direction;
		} else {
			// New column; do a descending sort
			this.propertyIndex = column;
			direction = DESCENDING;
		}
	}

	/**
	 * Must be Overriden in each view.
	 */
	public abstract int compare(Viewer viewer, Object e1, Object e2);
}
