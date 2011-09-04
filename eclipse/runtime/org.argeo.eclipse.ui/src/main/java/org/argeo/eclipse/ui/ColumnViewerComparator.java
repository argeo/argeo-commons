package org.argeo.eclipse.ui;

import java.util.Comparator;

import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;

/** Generic column viewer sortter */
public class ColumnViewerComparator<T> extends ViewerComparator {
	public static final int ASC = 1;

	public static final int NONE = 0;

	public static final int DESC = -1;

	private int direction = 0;

	private TableViewerColumn column;

	private ColumnViewer viewer;

	public ColumnViewerComparator(TableViewerColumn column,
			Comparator<T> comparator) {
		super(comparator);
		this.column = column;
		this.viewer = column.getViewer();
		this.column.getColumn().addSelectionListener(new SelectionAdapter() {

			public void widgetSelected(SelectionEvent e) {
				if (ColumnViewerComparator.this.viewer.getComparator() != null) {
					if (ColumnViewerComparator.this.viewer.getComparator() == ColumnViewerComparator.this) {
						int tdirection = ColumnViewerComparator.this.direction;

						if (tdirection == ASC) {
							setSortDirection(DESC);
						} else if (tdirection == DESC) {
							setSortDirection(NONE);
						}
					} else {
						setSortDirection(ASC);
					}
				} else {
					setSortDirection(ASC);
				}
			}
		});
	}

	private void setSortDirection(int direction) {
		if (direction == NONE) {
			column.getColumn().getParent().setSortColumn(null);
			column.getColumn().getParent().setSortDirection(SWT.NONE);
			viewer.setComparator(null);
		} else {
			column.getColumn().getParent().setSortColumn(column.getColumn());
			this.direction = direction;

			if (direction == ASC) {
				column.getColumn().getParent().setSortDirection(SWT.DOWN);
			} else {
				column.getColumn().getParent().setSortDirection(SWT.UP);
			}

			if (viewer.getComparator() == this) {
				viewer.refresh();
			} else {
				viewer.setComparator(this);
			}

		}
	}

	@SuppressWarnings("unchecked")
	public int compare(Viewer viewer, Object e1, Object e2) {
		return direction * getComparator().compare((T) e1, (T) e2);
	}
}
