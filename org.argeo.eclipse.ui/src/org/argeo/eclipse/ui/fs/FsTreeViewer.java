package org.argeo.eclipse.ui.fs;

import java.io.IOException;
import java.nio.file.DirectoryIteratorException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.argeo.eclipse.ui.ColumnDefinition;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;

/**
 * Canonical implementation of a JFace TreeViewer to display the content of a
 * repository
 */
public class FsTreeViewer extends TreeViewer {
	private static final long serialVersionUID = -5632407542678477234L;

	private boolean showHiddenItems = false;
	private boolean showDirectoryFirst = true;
	private String orderingProperty = FsUiConstants.PROPERTY_NAME;

	public FsTreeViewer(Composite parent, int style) {
		super(parent, style | SWT.VIRTUAL);
	}

	public Tree configureDefaultSingleColumnTable(int tableWidthHint) {
		Tree tree = this.getTree();
		tree.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		tree.setLinesVisible(true);
		tree.setHeaderVisible(false);
//		CmsUtils.markup(tree);

		TreeViewerColumn column = new TreeViewerColumn(this, SWT.NONE);
		TreeColumn tcol = column.getColumn();
		tcol.setWidth(tableWidthHint);
		column.setLabelProvider(new FileIconNameLabelProvider());

		this.setContentProvider(new MyCP());
		return tree;
	}

	public Tree configureDefaultTable(List<ColumnDefinition> columns) {
		this.setContentProvider(new MyCP());
		Tree tree = this.getTree();
		tree.setLinesVisible(true);
		tree.setHeaderVisible(true);
//		CmsUtils.markup(tree);
//		CmsUtils.style(tree, MaintenanceStyles.BROWSER_COLUMN);
		for (ColumnDefinition colDef : columns) {
			TreeViewerColumn column = new TreeViewerColumn(this, SWT.NONE);
			column.setLabelProvider(colDef.getLabelProvider());
			TreeColumn tcol = column.getColumn();
			tcol.setResizable(true);
			tcol.setText(colDef.getLabel());
			tcol.setWidth(colDef.getMinWidth());
		}
		return tree;
	}

	public void setInput(Path dir, String filter) {
		try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, filter)) {
			// TODO make this lazy
			List<Path> paths = new ArrayList<>();
			for (Path entry : stream) {
				paths.add(entry);
			}
			Object[] rows = paths.toArray(new Object[0]);
			this.setInput(rows);
			// this.setItemCount(rows.length);
			this.refresh();
		} catch (IOException | DirectoryIteratorException e) {
			throw new FsUiException("Unable to filter " + dir + " children with filter " + filter, e);
		}
	}

	/** Directly displays bookmarks **/
	public void setPathsInput(Path... paths) {
		this.setInput((Object[]) paths);
		// this.setItemCount(paths.length);
		this.refresh();
	}

	private class MyCP implements ITreeContentProvider {
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

		@Override
		public Object[] getElements(Object inputElement) {
			return elements;
		}

		@Override
		public Object[] getChildren(Object parentElement) {
			Path path = (Path) parentElement;
			if (!Files.isDirectory(path))
				return null;
			else
				return FsUiUtils.getChildren(path, "*", showHiddenItems, showDirectoryFirst, orderingProperty, false);
		}

		@Override
		public Object getParent(Object element) {
			Path path = (Path) element;
			return path.getParent();
		}

		@Override
		public boolean hasChildren(Object element) {
			Path path = (Path) element;
			try {
				if (!Files.isDirectory(path))
					return false;
				else
					try (DirectoryStream<Path> children = Files.newDirectoryStream(path, "*")) {
						return children.iterator().hasNext();
					}
			} catch (IOException e) {
				throw new FsUiException("Unable to check child existence on " + path, e);
			}
		}

	}
}
