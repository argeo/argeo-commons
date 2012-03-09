/*
 * Copyright (C) 2007-2012 Mathieu Baudier
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
package org.argeo.jcr.ui.explorer.editors;

import javax.jcr.Node;
import javax.jcr.security.AccessControlManager;
import javax.jcr.security.Privilege;

import org.argeo.ArgeoException;
import org.argeo.jcr.ui.explorer.JcrExplorerPlugin;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.ScrolledForm;

/**
 * This comments will be nicely fill by mbaudier in.
 */
public class NodeRightsManagementPage extends FormPage {
	// private final static Log log =
	// LogFactory.getLog(NodeRightsManagementPage.class);

	private Node currentNode;

	private TableViewer viewer;

	public NodeRightsManagementPage(FormEditor editor, String title,
			Node currentNode) {
		super(editor, "NodeRightsManagementPage", title);
		this.currentNode = currentNode;
	}

	protected void createFormContent(IManagedForm managedForm) {
		ScrolledForm form = managedForm.getForm();
		form.setText(JcrExplorerPlugin
				.getMessage("nodeRightsManagementPageTitle"));
		FillLayout layout = new FillLayout();
		layout.marginHeight = 5;
		layout.marginWidth = 5;
		form.getBody().setLayout(layout);
		createRightsPart(form.getBody());
	}

	/** Creates the rights part */
	protected void createRightsPart(Composite parent) {
		Table table = new Table(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
		table.setLinesVisible(true);
		table.setHeaderVisible(true);
		viewer = new TableViewer(table);

		// check column
		TableViewerColumn column = createTableViewerColumn(viewer, "checked",
				20);
		column.setLabelProvider(new ColumnLabelProvider() {
			public String getText(Object element) {
				return null;
			}

			public Image getImage(Object element) {
				return null;
			}
		});
		// column.setEditingSupport(new RoleEditingSupport(rolesViewer, part));

		// role column
		column = createTableViewerColumn(viewer, "Role", 200);
		column.setLabelProvider(new ColumnLabelProvider() {
			public String getText(Object element) {
				Privilege p = (Privilege) element;
				return p.getName();
			}

			public Image getImage(Object element) {
				return null;
			}
		});
		viewer.setContentProvider(new RightsContentProvider());
		viewer.setInput(getEditorSite());
	}

	protected TableViewerColumn createTableViewerColumn(TableViewer viewer,
			String title, int bound) {
		final TableViewerColumn viewerColumn = new TableViewerColumn(viewer,
				SWT.NONE);
		final TableColumn column = viewerColumn.getColumn();
		column.setText(title);
		column.setWidth(bound);
		column.setResizable(true);
		column.setMoveable(true);
		return viewerColumn;
	}

	private class RightsContentProvider implements IStructuredContentProvider {

		public void dispose() {
		}

		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		}

		public Object[] getElements(Object inputElement) {
			try {
				AccessControlManager accessControlManager = currentNode
						.getSession().getAccessControlManager();
				Privilege[] privileges = accessControlManager
						.getPrivileges(currentNode.getPath());
				return privileges;
			} catch (Exception e) {
				throw new ArgeoException("Cannot retrieve rights", e);
			}
		}

	}
}
