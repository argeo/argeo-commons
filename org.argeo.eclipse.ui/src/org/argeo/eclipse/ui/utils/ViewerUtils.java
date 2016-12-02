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
package org.argeo.eclipse.ui.utils;

import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TreeColumn;

/**
 * Centralizes useful methods to manage Jface Table, Tree and TreeColumn
 * viewers.
 */
public class ViewerUtils {

	/**
	 * Creates a basic column for the given table. For the time being, we do not
	 * support movable columns.
	 */
	public static TableColumn createColumn(Table parent, String name,
			int style, int width) {
		TableColumn result = new TableColumn(parent, style);
		result.setText(name);
		result.setWidth(width);
		result.setResizable(true);
		return result;
	}

	/**
	 * Creates a TableViewerColumn for the given viewer. For the time being, we
	 * do not support movable columns.
	 */
	public static TableViewerColumn createTableViewerColumn(TableViewer parent,
			String name, int style, int width) {
		TableViewerColumn tvc = new TableViewerColumn(parent, style);
		TableColumn column = tvc.getColumn();
		column.setText(name);
		column.setWidth(width);
		column.setResizable(true);
		return tvc;
	}

	/**
	 * Creates a TreeViewerColumn for the given viewer. For the time being, we
	 * do not support movable columns.
	 */
	public static TreeViewerColumn createTreeViewerColumn(TreeViewer parent,
			String name, int style, int width) {
		TreeViewerColumn tvc = new TreeViewerColumn(parent, style);
		TreeColumn column = tvc.getColumn();
		column.setText(name);
		column.setWidth(width);
		column.setResizable(true);
		return tvc;
	}
}
