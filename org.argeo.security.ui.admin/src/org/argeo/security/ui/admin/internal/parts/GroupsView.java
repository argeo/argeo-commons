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
package org.argeo.security.ui.admin.internal.parts;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.argeo.ArgeoException;
import org.argeo.eclipse.ui.ColumnDefinition;
import org.argeo.eclipse.ui.EclipseUiUtils;
import org.argeo.eclipse.ui.parts.LdifUsersTable;
import org.argeo.jcr.ArgeoNames;
import org.argeo.osgi.useradmin.LdifName;
import org.argeo.security.ui.admin.SecurityAdminPlugin;
import org.argeo.security.ui.admin.internal.UiAdminUtils;
import org.argeo.security.ui.admin.internal.UserAdminConstants;
import org.argeo.security.ui.admin.internal.UserAdminWrapper;
import org.argeo.security.ui.admin.internal.providers.CommonNameLP;
import org.argeo.security.ui.admin.internal.providers.DomainNameLP;
import org.argeo.security.ui.admin.internal.providers.RoleIconLP;
import org.argeo.security.ui.admin.internal.providers.UserDragListener;
import org.argeo.security.ui.admin.internal.providers.UserNameLP;
import org.argeo.security.ui.admin.internal.providers.UserTableDefaultDClickListener;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.part.ViewPart;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.useradmin.Role;
import org.osgi.service.useradmin.User;
import org.osgi.service.useradmin.UserAdminEvent;
import org.osgi.service.useradmin.UserAdminListener;

/** List all groups with filter */
public class GroupsView extends ViewPart implements ArgeoNames {
	private final static Log log = LogFactory.getLog(GroupsView.class);
	public final static String ID = SecurityAdminPlugin.PLUGIN_ID
			+ ".groupsView";

	/* DEPENDENCY INJECTION */
	private UserAdminWrapper userAdminWrapper;

	// UI Objects
	private LdifUsersTable groupTableViewerCmp;
	private TableViewer userViewer;
	private List<ColumnDefinition> columnDefs = new ArrayList<ColumnDefinition>();

	private UserAdminListener listener;

	@Override
	public void createPartControl(Composite parent) {
		parent.setLayout(EclipseUiUtils.noSpaceGridLayout());
		// Define the displayed columns
		columnDefs.add(new ColumnDefinition(new RoleIconLP(), "", 26));
		columnDefs.add(new ColumnDefinition(new CommonNameLP(), "Common Name",
				150));
		columnDefs.add(new ColumnDefinition(new DomainNameLP(), "Domain", 120));
		columnDefs.add(new ColumnDefinition(new UserNameLP(),
				"Distinguished Name", 300));

		// Create and configure the table
		groupTableViewerCmp = new MyUserTableViewer(parent, SWT.MULTI
				| SWT.H_SCROLL | SWT.V_SCROLL);

		groupTableViewerCmp.setColumnDefinitions(columnDefs);
		groupTableViewerCmp.populateWithStaticFilters(false, false);
		groupTableViewerCmp.setLayoutData(EclipseUiUtils.fillAll());

		// Links
		userViewer = groupTableViewerCmp.getTableViewer();
		userViewer.addDoubleClickListener(new UserTableDefaultDClickListener());
		getViewSite().setSelectionProvider(userViewer);

		// Really?
		groupTableViewerCmp.refresh();

		// Drag and drop
		int operations = DND.DROP_COPY | DND.DROP_MOVE;
		Transfer[] tt = new Transfer[] { TextTransfer.getInstance() };
		userViewer.addDragSupport(operations, tt, new UserDragListener(
				userViewer));

		// Register a useradmin listener
		listener = new UserAdminListener() {
			@Override
			public void roleChanged(UserAdminEvent event) {
				if (userViewer != null && !userViewer.getTable().isDisposed())
					refresh();
			}
		};
		userAdminWrapper.addListener(listener);
	}

	private class MyUserTableViewer extends LdifUsersTable {
		private static final long serialVersionUID = 8467999509931900367L;

		private Button showSystemRoleBtn;

		private final String[] knownProps = { LdifName.uid.name(),
				LdifName.cn.name(), LdifName.dn.name() };

		public MyUserTableViewer(Composite parent, int style) {
			super(parent, style);
		}

		protected void populateStaticFilters(Composite staticFilterCmp) {
			staticFilterCmp.setLayout(new GridLayout());
			showSystemRoleBtn = new Button(staticFilterCmp, SWT.CHECK);
			showSystemRoleBtn.setText("Show system roles");
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
				if (UiAdminUtils.notNull(filter))
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
								.append("=*")
								.append(UserAdminConstants.SYSTEM_ROLE_BASE_DN)
								.append("))");
					builder.append("(|");
					builder.append(tmpBuilder.toString());
					builder.append("))");
				} else {
					if (!showSystemRoleBtn.getSelection())
						builder.append("(&(objectclass=groupOfNames)(!(")
								.append(LdifName.dn.name()).append("=*")
								.append(UserAdminConstants.SYSTEM_ROLE_BASE_DN)
								.append(")))");
					else
						builder.append("(objectclass=groupOfNames)");

				}
				roles = userAdminWrapper.getUserAdmin().getRoles(
						builder.toString());
			} catch (InvalidSyntaxException e) {
				throw new ArgeoException("Unable to get roles with filter: "
						+ filter, e);
			}
			List<User> users = new ArrayList<User>();
			for (Role role : roles)
				if (!users.contains(role))
					users.add((User) role);
				else
					log.warn("Duplicated role: " + role);

			return users;
		}
	}

	public void refresh() {
		groupTableViewerCmp.refresh();
	}

	// Override generic view methods
	@Override
	public void dispose() {
		userAdminWrapper.removeListener(listener);
		super.dispose();
	}

	@Override
	public void setFocus() {
		groupTableViewerCmp.setFocus();
	}

	/* DEPENDENCY INJECTION */
	public void setUserAdminWrapper(UserAdminWrapper userAdminWrapper) {
		this.userAdminWrapper = userAdminWrapper;
	}
}