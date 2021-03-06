package org.argeo.eclipse.ui;

import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;

/** Generic column viewer sorter */
public class ColumnViewerComparator extends ViewerComparator {
	private static final long serialVersionUID = -2266218906355859909L;

	public static final int ASC = 1;

	public static final int NONE = 0;

	public static final int DESC = -1;

	private int direction = 0;

	private TableViewerColumn column;

	private ColumnViewer viewer;

	public ColumnViewerComparator(TableViewerColumn column) {
		super(null);
		this.column = column;
		this.viewer = column.getViewer();
		this.column.getColumn().addSelectionListener(new SelectionAdapter() {
			private static final long serialVersionUID = 7586796298965472189L;

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

	public int compare(Viewer viewer, Object e1, Object e2) {
		return direction * super.compare(viewer, e1, e2);
	}
}
