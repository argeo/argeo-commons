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

import org.argeo.ArgeoException;
import org.argeo.jcr.ArgeoNames;
import org.argeo.security.ui.admin.SecurityAdminPlugin;
import org.argeo.security.ui.admin.UserAdminConstants;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.useradmin.Role;
import org.osgi.service.useradmin.User;

/** List all groups with filter */
public class GroupsView extends UsersView implements ArgeoNames {
	public final static String ID = SecurityAdminPlugin.PLUGIN_ID
			+ ".groupsView";

	// The displayed columns
	/** Overwrite to display other columns */
	public List<ColumnDefinition> getColumnsDef() {
		List<ColumnDefinition> columnDefs = new ArrayList<ColumnDefinition>();
		// Group ID
		columnDefs.add(new ColumnDefinition(new UserNameLP(),
				"Distinguished Name", 200));
		// Displayed name
		columnDefs.add(new ColumnDefinition(new CommonNameLP(), "Common Name",
				150));
		return columnDefs;
	}

	/**
	 * Refresh the user list: caller might overwrite in order to display a
	 * subset of all users, typically to remove current user from the list
	 */
	protected void refreshFilteredList(String filter) {
		try {
			Role[] roles = userAdmin().getRoles(filter);
			List<User> users = new ArrayList<User>();
			for (Role role : roles)
				if (role.getType() == Role.GROUP)
					users.add((User) role);
			getViewer().setInput(users.toArray());
		} catch (InvalidSyntaxException e) {
			throw new ArgeoException("Unable to get roles with filter: "
					+ filter, e);
		}
	}

	private abstract class GroupAdminAbstractLP extends ColumnLabelProvider {
		private static final long serialVersionUID = 137336765024922368L;

		@Override
		public String getText(Object element) {
			User user = (User) element;
			return getText(user);
		}

		public abstract String getText(User user);
	}

	private class UserNameLP extends GroupAdminAbstractLP {
		private static final long serialVersionUID = 6550449442061090388L;

		@Override
		public String getText(User user) {
			return user.getName();
		}
	}

	private class CommonNameLP extends GroupAdminAbstractLP {
		private static final long serialVersionUID = 5256703081044911941L;

		@Override
		public String getText(User user) {
			Object obj = user.getProperties().get(UserAdminConstants.KEY_CN);
			if (obj != null)
				return (String) obj;
			else
				return "";
		}
	}

}