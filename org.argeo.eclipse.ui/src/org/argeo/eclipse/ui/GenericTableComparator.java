/*
 * Copyright (C) 2007-2012 Argeo GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.argeo.eclipse.ui;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;

public abstract class GenericTableComparator extends ViewerComparator {
	private static final long serialVersionUID = -1175894935075325810L;
	protected int propertyIndex;
	public static final int ASCENDING = 0, DESCENDING = 1;
	protected int direction = DESCENDING;

	/**
	 * Creates an instance of a sorter for TableViewer.
	 * 
	 * @param defaultColumnIndex
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
