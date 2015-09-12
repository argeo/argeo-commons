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

import java.util.ArrayList;
import java.util.List;

import javax.transaction.UserTransaction;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.argeo.ArgeoException;
import org.argeo.eclipse.ui.EclipseUiUtils;
import org.argeo.jcr.ArgeoNames;
import org.argeo.security.ui.admin.SecurityAdminPlugin;
import org.argeo.security.ui.admin.internal.ColumnDefinition;
import org.argeo.security.ui.admin.internal.CommonNameLP;
import org.argeo.security.ui.admin.internal.MailLP;
import org.argeo.security.ui.admin.internal.UserNameLP;
import org.argeo.security.ui.admin.internal.UserTableDefaultDClickListener;
import org.argeo.security.ui.admin.internal.UserTableViewer;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.part.ViewPart;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.useradmin.Role;
import org.osgi.service.useradmin.User;
import org.osgi.service.useradmin.UserAdmin;

/** List all users with filter - based on Ldif userAdmin */
public class UsersView extends ViewPart implements ArgeoNames {
	private final static Log log = LogFactory.getLog(UsersView.class);
	public final static String ID = SecurityAdminPlugin.PLUGIN_ID
			+ ".usersView";

	/* DEPENDENCY INJECTION */
	private UserAdmin userAdmin;
	private UserTransaction userTransaction;

	// UI Objects
	private UserTableViewer userTableViewerCmp;
	private TableViewer userViewer;
	private List<ColumnDefinition> columnDefs = new ArrayList<ColumnDefinition>();

	@Override
	public void createPartControl(Composite parent) {
		parent.setLayout(EclipseUiUtils.noSpaceGridLayout());
		// Define the displayed columns
		columnDefs.add(new ColumnDefinition(new CommonNameLP(), "Common Name",
				150));
		columnDefs.add(new ColumnDefinition(new MailLP(), "E-mail", 150));
		columnDefs.add(new ColumnDefinition(new UserNameLP(),
				"Distinguished Name", 300));

		// Create and configure the table
		userTableViewerCmp = new MyUserTableViewer(parent, SWT.MULTI
				| SWT.H_SCROLL | SWT.V_SCROLL, userAdmin);
		userTableViewerCmp.setLayoutData(EclipseUiUtils.fillAll());

		userTableViewerCmp.setColumnDefinitions(columnDefs);
		userTableViewerCmp.populate(true, false);

		// Links
		userViewer = userTableViewerCmp.getTableViewer();
		userViewer.addDoubleClickListener(new UserTableDefaultDClickListener());
		getViewSite().setSelectionProvider(userViewer);

		// Really?
		userTableViewerCmp.refresh();

		try {
			if (userTransaction != null)
				userTransaction.begin();
		} catch (Exception e) {
			throw new ArgeoException("Cannot begin transaction", e);
		}
	}

	private class MyUserTableViewer extends UserTableViewer {
		private static final long serialVersionUID = 8467999509931900367L;

		public MyUserTableViewer(Composite parent, int style,
				UserAdmin userAdmin) {
			super(parent, style, userAdmin);
		}

		@Override
		protected List<User> listFilteredElements(String filter) {
			Role[] roles;
			try {
				roles = userAdmin.getRoles(filter);
			} catch (InvalidSyntaxException e) {
				throw new ArgeoException("Unable to get roles with filter: "
						+ filter, e);
			}
			List<User> users = new ArrayList<User>();
			for (Role role : roles)
				if (role.getType() == Role.USER && role.getType() != Role.GROUP)
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
		super.dispose();
		// try {
		// if (userTransaction != null
		// && userTransaction.getStatus() != Status.STATUS_NO_TRANSACTION)
		// userTransaction.rollback();
		// } catch (Exception e) {
		// log.error("Cannot clean transaction", e);
		// }
	}

	@Override
	public void setFocus() {
		userTableViewerCmp.setFocus();
	}

	/* DEPENDENCY INJECTION */
	public void setUserAdmin(UserAdmin userAdmin) {
		this.userAdmin = userAdmin;
	}

	public void setUserTransaction(UserTransaction userTransaction) {
		this.userTransaction = userTransaction;
	}

}