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
package org.argeo.cms.ui.workbench.internal.useradmin.parts;

import java.util.ArrayList;
import java.util.List;

import org.argeo.cms.ArgeoNames;
import org.argeo.cms.CmsException;
import org.argeo.cms.auth.CurrentUser;
import org.argeo.cms.ui.workbench.WorkbenchUiPlugin;
import org.argeo.cms.ui.workbench.internal.useradmin.UiUserAdminListener;
import org.argeo.cms.ui.workbench.internal.useradmin.UserAdminWrapper;
import org.argeo.cms.ui.workbench.internal.useradmin.providers.CommonNameLP;
import org.argeo.cms.ui.workbench.internal.useradmin.providers.DomainNameLP;
import org.argeo.cms.ui.workbench.internal.useradmin.providers.MailLP;
import org.argeo.cms.ui.workbench.internal.useradmin.providers.UserDragListener;
import org.argeo.cms.ui.workbench.internal.useradmin.providers.UserNameLP;
import org.argeo.cms.ui.workbench.internal.useradmin.providers.UserTableDefaultDClickListener;
import org.argeo.eclipse.ui.ColumnDefinition;
import org.argeo.eclipse.ui.EclipseUiUtils;
import org.argeo.eclipse.ui.parts.LdifUsersTable;
import org.argeo.naming.LdapAttrs;
import org.argeo.naming.LdapObjs;
import org.argeo.node.NodeConstants;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.part.ViewPart;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.useradmin.Role;
import org.osgi.service.useradmin.User;
import org.osgi.service.useradmin.UserAdminEvent;
import org.osgi.service.useradmin.UserAdminListener;

/** List all users with filter - based on Ldif userAdmin */
public class UsersView extends ViewPart implements ArgeoNames {
	// private final static Log log = LogFactory.getLog(UsersView.class);

	public final static String ID = WorkbenchUiPlugin.PLUGIN_ID + ".usersView";

	/* DEPENDENCY INJECTION */
	private UserAdminWrapper userAdminWrapper;

	// UI Objects
	private LdifUsersTable userTableViewerCmp;
	private TableViewer userViewer;
	private List<ColumnDefinition> columnDefs = new ArrayList<ColumnDefinition>();

	private UserAdminListener listener;

	@Override
	public void createPartControl(Composite parent) {

		parent.setLayout(EclipseUiUtils.noSpaceGridLayout());
		// Define the displayed columns
		columnDefs.add(new ColumnDefinition(new CommonNameLP(), "Common Name",
				150));
		columnDefs.add(new ColumnDefinition(new MailLP(), "E-mail", 150));
		columnDefs.add(new ColumnDefinition(new DomainNameLP(), "Domain", 200));
		// Only show technical DN to admin
		if (CurrentUser.isInRole(NodeConstants.ROLE_ADMIN))
			columnDefs.add(new ColumnDefinition(new UserNameLP(),
					"Distinguished Name", 300));

		// Create and configure the table
		userTableViewerCmp = new MyUserTableViewer(parent, SWT.MULTI
				| SWT.H_SCROLL | SWT.V_SCROLL);
		userTableViewerCmp.setLayoutData(EclipseUiUtils.fillAll());
		userTableViewerCmp.setColumnDefinitions(columnDefs);
		userTableViewerCmp.populate(true, false);

		// Links
		userViewer = userTableViewerCmp.getTableViewer();
		userViewer.addDoubleClickListener(new UserTableDefaultDClickListener());
		getViewSite().setSelectionProvider(userViewer);

		// Really?
		userTableViewerCmp.refresh();

		// Drag and drop
		int operations = DND.DROP_COPY | DND.DROP_MOVE;
		Transfer[] tt = new Transfer[] { TextTransfer.getInstance() };
		userViewer.addDragSupport(operations, tt, new UserDragListener(
				userViewer));

		// Register a useradmin listener
		listener = new MyUiUAListener(parent.getDisplay());
		userAdminWrapper.addListener(listener);
	}

	private class MyUiUAListener extends UiUserAdminListener {
		public MyUiUAListener(Display display) {
			super(display);
		}

		@Override
		public void roleChangedToUiThread(UserAdminEvent event) {
			if (userViewer != null && !userViewer.getTable().isDisposed())
				refresh();
		}
	}

	private class MyUserTableViewer extends LdifUsersTable {
		private static final long serialVersionUID = 8467999509931900367L;

		private final String[] knownProps = { LdapAttrs.DN,
				LdapAttrs.uid.name(), LdapAttrs.cn.name(),
				LdapAttrs.givenName.name(), LdapAttrs.sn.name(),
				LdapAttrs.mail.name() };

		public MyUserTableViewer(Composite parent, int style) {
			super(parent, style);
		}

		@Override
		protected List<User> listFilteredElements(String filter) {
			Role[] roles;

			try {
				StringBuilder builder = new StringBuilder();

				StringBuilder tmpBuilder = new StringBuilder();
				if (EclipseUiUtils.notEmpty(filter))
					for (String prop : knownProps) {
						tmpBuilder.append("(");
						tmpBuilder.append(prop);
						tmpBuilder.append("=*");
						tmpBuilder.append(filter);
						tmpBuilder.append("*)");
					}
				if (tmpBuilder.length() > 1) {
					builder.append("(&(").append(LdapAttrs.objectClass.name())
							.append("=").append(LdapObjs.inetOrgPerson.name())
							.append(")(|");
					builder.append(tmpBuilder.toString());
					builder.append("))");
				} else
					builder.append("(").append(LdapAttrs.objectClass.name())
							.append("=").append(LdapObjs.inetOrgPerson.name())
							.append(")");
				roles = userAdminWrapper.getUserAdmin().getRoles(
						builder.toString());
			} catch (InvalidSyntaxException e) {
				throw new CmsException("Unable to get roles with filter: "
						+ filter, e);
			}
			List<User> users = new ArrayList<User>();
			for (Role role : roles)
				// if (role.getType() == Role.USER && role.getType() !=
				// Role.GROUP)
				users.add((User) role);
			return users;
		}
	}

	public void refresh() {
		userTableViewerCmp.refresh();
	}

	// Override generic view methods
	@Override
	public void dispose() {
		userAdminWrapper.removeListener(listener);
		super.dispose();
	}

	@Override
	public void setFocus() {
		userTableViewerCmp.setFocus();
	}

	/* DEPENDENCY INJECTION */
	public void setUserAdminWrapper(UserAdminWrapper userAdminWrapper) {
		this.userAdminWrapper = userAdminWrapper;
	}
}
