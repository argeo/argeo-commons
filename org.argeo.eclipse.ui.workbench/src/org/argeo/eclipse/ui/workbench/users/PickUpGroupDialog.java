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
package org.argeo.eclipse.ui.workbench.users;

import java.util.ArrayList;
import java.util.List;

import org.argeo.ArgeoException;
import org.argeo.eclipse.ui.ColumnDefinition;
import org.argeo.eclipse.ui.EclipseUiUtils;
import org.argeo.eclipse.ui.parts.LdifUsersTable;
import org.argeo.osgi.useradmin.LdifName;
import org.eclipse.jface.dialogs.TrayDialog;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.useradmin.Group;
import org.osgi.service.useradmin.Role;
import org.osgi.service.useradmin.User;
import org.osgi.service.useradmin.UserAdmin;

/** Dialog with a group list to pick up one */
public class PickUpGroupDialog extends TrayDialog {
	private static final long serialVersionUID = -1420106871173920369L;

	// Business objects
	private final UserAdmin userAdmin;
	private Group selectedGroup;

	// this page widgets and UI objects
	private String title;
	private LdifUsersTable groupTableViewerCmp;
	private TableViewer userViewer;
	private List<ColumnDefinition> columnDefs = new ArrayList<ColumnDefinition>();

	public PickUpGroupDialog(Shell parentShell, String title,
			UserAdmin userAdmin) {
		super(parentShell);
		this.title = title;
		this.userAdmin = userAdmin;

		// Define the displayed columns
		columnDefs.add(new ColumnDefinition(new GroupLP(GroupLP.COL_ICON), "",
				26, 0));
		columnDefs.add(new ColumnDefinition(new GroupLP(
				GroupLP.COL_DISPLAY_NAME), "Common Name", 150, 100));
		columnDefs.add(new ColumnDefinition(new GroupLP(GroupLP.COL_DOMAIN),
				"Domain", 100, 120));
		columnDefs.add(new ColumnDefinition(new GroupLP(GroupLP.COL_DN),
				"Distinguished Name", 300, 100));
	}

	protected Point getInitialSize() {
		return new Point(600, 450);
	}

	protected Control createDialogArea(Composite parent) {
		Composite dialogArea = (Composite) super.createDialogArea(parent);
		dialogArea.setLayout(new FillLayout());

		Composite bodyCmp = new Composite(dialogArea, SWT.NO_FOCUS);
		bodyCmp.setLayout(new GridLayout());

		// Create and configure the table
		groupTableViewerCmp = new MyUserTableViewer(parent, SWT.MULTI
				| SWT.H_SCROLL | SWT.V_SCROLL);

		groupTableViewerCmp.setColumnDefinitions(columnDefs);
		groupTableViewerCmp.populateWithStaticFilters(false, false);
		groupTableViewerCmp.setLayoutData(EclipseUiUtils.fillAll());
		groupTableViewerCmp.refresh();

		// Controllers
		userViewer = groupTableViewerCmp.getTableViewer();
		userViewer.addDoubleClickListener(new MyDoubleClickListener());
		userViewer
				.addSelectionChangedListener(new MySelectionChangedListener());

		parent.pack();
		return dialogArea;
	}

	public String getSelected() {
		if (selectedGroup == null)
			return null;
		else
			return selectedGroup.getName();
	}

	protected void configureShell(Shell shell) {
		super.configureShell(shell);
		shell.setText(title);
	}

	class MyDoubleClickListener implements IDoubleClickListener {
		public void doubleClick(DoubleClickEvent evt) {
			if (evt.getSelection().isEmpty())
				return;

			Object obj = ((IStructuredSelection) evt.getSelection())
					.getFirstElement();
			if (obj instanceof Group) {
				selectedGroup = (Group) obj;
				okPressed();
			}
		}
	}

	class MySelectionChangedListener implements ISelectionChangedListener {
		@Override
		public void selectionChanged(SelectionChangedEvent event) {
			if (event.getSelection().isEmpty()) {
				selectedGroup = null;
				return;
			}
			Object obj = ((IStructuredSelection) event.getSelection())
					.getFirstElement();
			if (obj instanceof Group) {
				selectedGroup = (Group) obj;
			}
		}
	}

	private class MyUserTableViewer extends LdifUsersTable {
		private static final long serialVersionUID = 8467999509931900367L;

		private final String[] knownProps = { LdifName.uid.name(),
				LdifName.cn.name(), LdifName.dn.name() };

		private Button showSystemRoleBtn;

		public MyUserTableViewer(Composite parent, int style) {
			super(parent, style);
		}

		protected void populateStaticFilters(Composite staticFilterCmp) {
			staticFilterCmp.setLayout(new GridLayout());
			showSystemRoleBtn = new Button(staticFilterCmp, SWT.CHECK);
			showSystemRoleBtn.setText("Show system roles  ");
			showSystemRoleBtn.addSelectionListener(new SelectionAdapter() {
				private static final long serialVersionUID = -7033424592697691676L;

				@Override
				public void widgetSelected(SelectionEvent e) {
					refresh();
				}

			});
		}

		@Override
		protected List<User> listFilteredElements(String filter) {
			Role[] roles;
			try {
				StringBuilder builder = new StringBuilder();
				StringBuilder tmpBuilder = new StringBuilder();
				if (notNull(filter))
					for (String prop : knownProps) {
						tmpBuilder.append("(");
						tmpBuilder.append(prop);
						tmpBuilder.append("=*");
						tmpBuilder.append(filter);
						tmpBuilder.append("*)");
					}
				if (tmpBuilder.length() > 1) {
					builder.append("(&(objectclass=groupOfNames)");
					if (!showSystemRoleBtn.getSelection())
						builder.append("(!(").append(LdifName.dn.name())
								.append("=*").append(GroupLP.ROLES_BASEDN)
								.append("))");
					builder.append("(|");
					builder.append(tmpBuilder.toString());
					builder.append("))");
				} else {
					if (!showSystemRoleBtn.getSelection())
						builder.append("(&(objectclass=groupOfNames)(!(")
								.append(LdifName.dn.name()).append("=*")
								.append(GroupLP.ROLES_BASEDN).append(")))");
					else
						builder.append("(objectclass=groupOfNames)");

				}
				roles = userAdmin.getRoles(builder.toString());
			} catch (InvalidSyntaxException e) {
				throw new ArgeoException("Unable to get roles with filter: "
						+ filter, e);
			}
			List<User> users = new ArrayList<User>();
			for (Role role : roles)
				users.add((User) role);
			return users;
		}
	}

	private boolean notNull(String string) {
		if (string == null)
			return false;
		else
			return !"".equals(string.trim());
	}
}