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
package org.argeo.security.ui.admin.views;

import org.argeo.ArgeoException;
import org.argeo.security.UserAdminService;
import org.argeo.security.ui.admin.SecurityAdminPlugin;
import org.argeo.security.ui.admin.commands.AddRole;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.handlers.IHandlerService;
import org.eclipse.ui.part.ViewPart;

/** List all roles. */
public class RolesView extends ViewPart {
	public final static String ID = SecurityAdminPlugin.PLUGIN_ID
			+ ".adminRolesView";

	private Text newRole;

	private TableViewer viewer;
	private UserAdminService userAdminService;

	private String addNewRoleText = "<add new role here>";

	@Override
	public void createPartControl(Composite parent) {
		parent.setLayout(new GridLayout(1, false));

		// new role text field
		newRole = new Text(parent, SWT.BORDER);
		newRole.setText(addNewRoleText);
		newRole.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		// default action is add role
		newRole.addListener(SWT.DefaultSelection, new Listener() {
			public void handleEvent(Event evt) {
				IWorkbench iw = SecurityAdminPlugin.getDefault().getWorkbench();
				IHandlerService handlerService = (IHandlerService) iw
						.getService(IHandlerService.class);
				try {
					handlerService.executeCommand(AddRole.COMMAND_ID, evt);
				} catch (Exception e) {
					throw new ArgeoException("Cannot execute add role command",
							e);
				}
			}
		});
		// select all on focus
		newRole.addListener(SWT.FocusIn, new Listener() {
			public void handleEvent(Event e) {
				newRole.selectAll();
			}
		});

		// roles table
		Table table = new Table(parent, SWT.V_SCROLL | SWT.BORDER);
		table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		table.setLinesVisible(false);
		table.setHeaderVisible(false);
		viewer = new TableViewer(table);
		viewer.setContentProvider(new RolesContentProvider());
		viewer.setLabelProvider(new UsersLabelProvider());
		getViewSite().setSelectionProvider(viewer);
		viewer.setInput(getViewSite());
	}

	@Override
	public void setFocus() {
		viewer.getTable().setFocus();
	}

	public void setUserAdminService(UserAdminService userAdminService) {
		this.userAdminService = userAdminService;
	}

	public String getAddNewRoleText() {
		return addNewRoleText;
	}

	private class RolesContentProvider implements IStructuredContentProvider {

		public Object[] getElements(Object inputElement) {
			return userAdminService.listEditableRoles().toArray();
		}

		public void dispose() {
		}

		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		}

	}

	private class UsersLabelProvider extends LabelProvider implements
			ITableLabelProvider {
		public String getColumnText(Object element, int columnIndex) {
			return element.toString();
		}

		public Image getColumnImage(Object element, int columnIndex) {
			return null;
		}

	}

	public String getNewRole() {
		return newRole.getText();
	}

	public void refresh() {
		viewer.refresh();
		newRole.setText(addNewRoleText);
	}
}
