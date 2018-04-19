package org.argeo.eclipse.ui.fs;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.argeo.eclipse.ui.ColumnDefinition;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ILazyContentProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;

/**
 * Canonical implementation of a JFace table viewer to display the content of a
 * file folder
 */
public class FsTableViewer extends TableViewer {
	private static final long serialVersionUID = -5632407542678477234L;

	private boolean showHiddenItems = false;
	private boolean folderFirst = true;
	private boolean reverseOrder = false;
	private String orderProperty = FsUiConstants.PROPERTY_NAME;

	public FsTableViewer(Composite parent, int style) {
		super(parent, style | SWT.VIRTUAL);
	}

	public Table configureDefaultSingleColumnTable(int tableWidthHint) {

		return configureDefaultSingleColumnTable(tableWidthHint, new FileIconNameLabelProvider());
	}

	public Table configureDefaultSingleColumnTable(int tableWidthHint, CellLabelProvider labelProvider) {
		Table table = this.getTable();
		table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		table.setLinesVisible(false);
		table.setHeaderVisible(false);
		// CmsUtils.markup(table);
		// CmsUtils.style(table, MaintenanceStyles.BROWSER_COLUMN);

		TableViewerColumn column = new TableViewerColumn(this, SWT.NONE);
		TableColumn tcol = column.getColumn();
		tcol.setWidth(tableWidthHint);
		column.setLabelProvider(labelProvider);
		this.setContentProvider(new MyLazyCP());
		return table;
	}

	public Table configureDefaultTable(List<ColumnDefinition> columns) {
		this.setContentProvider(new MyLazyCP());
		Table table = this.getTable();
		table.setLinesVisible(true);
		table.setHeaderVisible(true);
		// CmsUtils.markup(table);
		// CmsUtils.style(table, MaintenanceStyles.BROWSER_COLUMN);
		for (ColumnDefinition colDef : columns) {
			TableViewerColumn column = new TableViewerColumn(this, SWT.NONE);
			column.setLabelProvider(colDef.getLabelProvider());
			TableColumn tcol = column.getColumn();
			tcol.setResizable(true);
			tcol.setText(colDef.getLabel());
			tcol.setWidth(colDef.getMinWidth());
		}
		return table;
	}

	public void setInput(Path dir, String filter) {
		Path[] rows = FsUiUtils.getChildren(dir, filter, showHiddenItems, folderFirst, orderProperty, reverseOrder);
		if (rows == null) {
			this.setInput(null);
			this.setItemCount(0);
			return;
		}
		boolean isRoot;
		try {
			isRoot = dir.getRoot().equals(dir);
		} catch (Exception e) {
			// FIXME Workaround for JCR root node access
			isRoot = dir.toString().equals("/");
		}
		final Object[] res;
		if (isRoot)
			res = rows;
		else {
			res = new Object[rows.length + 1];
			res[0] = new ParentDir(dir.getParent());
			for (int i = 1; i < res.length; i++) {
				res[i] = rows[i - 1];
			}
		}
		this.setInput(res);
		int length = res.length;
		this.setItemCount(length);
		this.refresh();
	}

	/** Directly displays bookmarks **/
	public void setPathsInput(Path... paths) {
		this.setInput((Object[]) paths);
		this.setItemCount(paths.length);
		this.refresh();
	}

	private class MyLazyCP implements ILazyContentProvider {
		private static final long serialVersionUID = 9096550041395433128L;
		private Object[] elements;

		public void dispose() {
		}

		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			// IMPORTANT: don't forget this: an exception will be thrown if
			// a selected object is not part of the results anymore.
			viewer.setSelection(null);
			this.elements = (Object[]) newInput;
		}

		public void updateElement(int index) {
			if (index < elements.length)
				FsTableViewer.this.replace(elements[index], index);
		}
	}
}
