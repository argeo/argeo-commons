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
package org.argeo.cms.ui.useradmin;

import java.util.ArrayList;
import java.util.List;

import org.argeo.eclipse.ui.ColumnDefinition;
import org.argeo.eclipse.ui.EclipseUiException;
import org.argeo.eclipse.ui.EclipseUiUtils;
import org.argeo.eclipse.ui.parts.LdifUsersTable;
import org.argeo.naming.LdapAttrs;
import org.argeo.naming.LdapObjs;
import org.argeo.node.NodeConstants;
import org.eclipse.jface.dialogs.MessageDialog;
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
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
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

/** Dialog with a user (or group) list to pick up one */
public class PickUpUserDialog extends TrayDialog {
	private static final long serialVersionUID = -1420106871173920369L;

	// Business objects
	private final UserAdmin userAdmin;
	private User selectedUser;

	// this page widgets and UI objects
	private String title;
	private LdifUsersTable userTableViewerCmp;
	private TableViewer userViewer;
	private List<ColumnDefinition> columnDefs = new ArrayList<ColumnDefinition>();

	/**
	 * A dialog to pick up a group or a user, showing a table with default
	 * columns
	 */
	public PickUpUserDialog(Shell parentShell, String title, UserAdmin userAdmin) {
		super(parentShell);
		this.title = title;
		this.userAdmin = userAdmin;

		columnDefs.add(new ColumnDefinition(new UserLP(UserLP.COL_ICON), "",
				24, 24));
		columnDefs.add(new ColumnDefinition(
				new UserLP(UserLP.COL_DISPLAY_NAME), "Common Name", 150, 100));
		columnDefs.add(new ColumnDefinition(new UserLP(UserLP.COL_DOMAIN),
				"Domain", 100, 120));
		columnDefs.add(new ColumnDefinition(new UserLP(UserLP.COL_DN),
				"Distinguished Name", 300, 100));
	}

	/** A dialog to pick up a group or a user */
	public PickUpUserDialog(Shell parentShell, String title,
			UserAdmin userAdmin, List<ColumnDefinition> columnDefs) {
		super(parentShell);
		this.title = title;
		this.userAdmin = userAdmin;
		this.columnDefs = columnDefs;
	}

	@Override
	protected void okPressed() {
		if (getSelected() == null)
			MessageDialog.openError(getShell(), "No user chosen",
					"Please, choose a user or press Cancel.");
		else
			super.okPressed();
	}

	protected Control createDialogArea(Composite parent) {
		Composite dialogArea = (Composite) super.createDialogArea(parent);
		dialogArea.setLayout(new FillLayout());

		Composite bodyCmp = new Composite(dialogArea, SWT.NO_FOCUS);
		bodyCmp.setLayout(new GridLayout());

		// Create and configure the table
		userTableViewerCmp = new MyUserTableViewer(bodyCmp, SWT.MULTI
				| SWT.H_SCROLL | SWT.V_SCROLL);

		userTableViewerCmp.setColumnDefinitions(columnDefs);
		userTableViewerCmp.populateWithStaticFilters(false, false);
		GridData gd = EclipseUiUtils.fillAll();
		gd.minimumHeight = 300;
		userTableViewerCmp.setLayoutData(gd);
		userTableViewerCmp.refresh();

		// Controllers
		userViewer = userTableViewerCmp.getTableViewer();
		userViewer.addDoubleClickListener(new MyDoubleClickListener());
		userViewer
				.addSelectionChangedListener(new MySelectionChangedListener());

		parent.pack();
		return dialogArea;
	}

	public User getSelected() {
		if (selectedUser == null)
			return null;
		else
			return selectedUser;
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
			if (obj instanceof User) {
				selectedUser = (User) obj;
				okPressed();
			}
		}
	}

	class MySelectionChangedListener implements ISelectionChangedListener {
		@Override
		public void selectionChanged(SelectionChangedEvent event) {
			if (event.getSelection().isEmpty()) {
				selectedUser = null;
				return;
			}
			Object obj = ((IStructuredSelection) event.getSelection())
					.getFirstElement();
			if (obj instanceof Group) {
				selectedUser = (Group) obj;
			}
		}
	}

	private class MyUserTableViewer extends LdifUsersTable {
		private static final long serialVersionUID = 8467999509931900367L;

		private final String[] knownProps = { LdapAttrs.uid.name(),
				LdapAttrs.cn.name(), LdapAttrs.DN };

		private Button showSystemRoleBtn;
		private Button showUserBtn;

		public MyUserTableViewer(Composite parent, int style) {
			super(parent, style);
		}

		protected void populateStaticFilters(Composite staticFilterCmp) {
			staticFilterCmp.setLayout(new GridLayout());
			showSystemRoleBtn = new Button(staticFilterCmp, SWT.CHECK);
			showSystemRoleBtn.setText("Show system roles  ");

			showUserBtn = new Button(staticFilterCmp, SWT.CHECK);
			showUserBtn.setText("Show users  ");

			SelectionListener sl = new SelectionAdapter() {
				private static final long serialVersionUID = -7033424592697691676L;

				@Override
				public void widgetSelected(SelectionEvent e) {
					refresh();
				}
			};

			showSystemRoleBtn.addSelectionListener(sl);
			showUserBtn.addSelectionListener(sl);
		}

		@Override
		protected List<User> listFilteredElements(String filter) {
			Role[] roles;
			try {
				StringBuilder builder = new StringBuilder();

				StringBuilder filterBuilder = new StringBuilder();
				if (notNull(filter))
					for (String prop : knownProps) {
						filterBuilder.append("(");
						filterBuilder.append(prop);
						filterBuilder.append("=*");
						filterBuilder.append(filter);
						filterBuilder.append("*)");
					}

				String typeStr = "(" + LdapAttrs.objectClass.name() + "="
						+ LdapObjs.groupOfNames.name() + ")";
				if ((showUserBtn.getSelection()))
					typeStr = "(|(" + LdapAttrs.objectClass.name() + "="
							+ LdapObjs.inetOrgPerson.name() + ")" + typeStr
							+ ")";

				if (!showSystemRoleBtn.getSelection())
					typeStr = "(& " + typeStr + "(!(" + LdapAttrs.DN + "=*"
							+ NodeConstants.ROLES_BASEDN + ")))";

				if (filterBuilder.length() > 1) {
					builder.append("(&" + typeStr);
					builder.append("(|");
					builder.append(filterBuilder.toString());
					builder.append("))");
				} else {
					builder.append(typeStr);
				}
				roles = userAdmin.getRoles(builder.toString());
			} catch (InvalidSyntaxException e) {
				throw new EclipseUiException(
						"Unable to get roles with filter: " + filter, e);
			}
			List<User> users = new ArrayList<User>();
			for (Role role : roles)
				if (!users.contains(role))
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
