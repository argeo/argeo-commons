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
import org.argeo.eclipse.ui.EclipseUiUtils;
import org.argeo.eclipse.ui.utils.ViewerUtils;
import org.argeo.jcr.ArgeoNames;
import org.argeo.security.ui.admin.SecurityAdminPlugin;
import org.argeo.security.ui.admin.editors.UserEditor;
import org.argeo.security.ui.admin.editors.UserEditorInput;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.ViewPart;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.useradmin.Role;
import org.osgi.service.useradmin.User;
import org.osgi.service.useradmin.UserAdmin;

/** List all users with filter - based on Ldif userAdmin */
public class UsersView extends ViewPart implements ArgeoNames {
	public final static String ID = SecurityAdminPlugin.PLUGIN_ID
			+ ".usersView";

	/* DEPENDENCY INJECTION */
	private UserAdmin userAdmin;

	// UI Objects
	private TableViewer usersViewer;
	private Text filterTxt;
	private Font italic;
	private Font bold;

	// The displayed columns
	/** Overwrite to display other columns */
	public List<ColumnDefinition> getColumnsDef() {
		List<ColumnDefinition> columnDefs = new ArrayList<ColumnDefinition>();

		// User ID
		columnDefs.add(new ColumnDefinition(new UserNameLP(),
				"Distinguished Name", 200));
		// Displayed name
		columnDefs.add(new ColumnDefinition(new CommonNameLP(), "Common Name",
				150));
		// E-mail
		columnDefs.add(new ColumnDefinition(new MailLP(), "E-mail", 150));
		return columnDefs;
	}

	@Override
	public void setFocus() {
		// TODO Auto-generated method stub
	}

	protected Viewer getViewer() {
		return usersViewer;
	}

	@Override
	public void createPartControl(Composite parent) {
		// cache UI Objects
		italic = EclipseUiUtils.getItalicFont(parent);
		bold = EclipseUiUtils.getBoldFont(parent);

		// Main Layout
		GridLayout layout = EclipseUiUtils.noSpaceGridLayout();
		layout.verticalSpacing = 5;
		parent.setLayout(layout);

		usersViewer = createTableViewer(parent);
		usersViewer.setContentProvider(new UsersContentProvider());

		// Really?
		refreshFilteredList(null);

		// Configure
		usersViewer.addDoubleClickListener(new ViewDoubleClickListener());
		getViewSite().setSelectionProvider(usersViewer);
	}

	public void refresh() {
		this.getSite().getShell().getDisplay().asyncExec(new Runnable() {
			@Override
			public void run() {
				refreshFilteredList(null);
			}
		});
	}

	class ViewDoubleClickListener implements IDoubleClickListener {
		public void doubleClick(DoubleClickEvent evt) {
			if (evt.getSelection().isEmpty())
				return;
			Object obj = ((IStructuredSelection) evt.getSelection())
					.getFirstElement();
			User user = (User) obj;
			IWorkbenchWindow iww = UsersView.this.getSite()
					.getWorkbenchWindow();
			IWorkbenchPage iwp = iww.getActivePage();
			UserEditorInput uei = new UserEditorInput(user.getName());

			try {
				// IEditorPart editor =
				iwp.openEditor(uei, UserEditor.ID);
			} catch (PartInitException pie) {
				throw new ArgeoException("Unable to open UserEditor for "
						+ user, pie);
			}
		}
	}

	private TableViewer createTableViewer(final Composite parent) {
		int style = SWT.H_SCROLL | SWT.V_SCROLL;

		Composite tableCmp = new Composite(parent, SWT.NO_FOCUS);
		tableCmp.setLayoutData(EclipseUiUtils.fillAll());

		Table table = new Table(tableCmp, style);
		TableViewer viewer = new TableViewer(table);
		table.setLinesVisible(true);
		table.setHeaderVisible(true);

		TableColumnLayout tableColumnLayout = new TableColumnLayout();
		TableViewerColumn column;

		// Create other columns
		List<ColumnDefinition> colDefs = getColumnsDef();
		for (ColumnDefinition colDef : colDefs) {
			column = ViewerUtils.createTableViewerColumn(viewer, colDef.label,
					SWT.NONE, colDef.weight);
			column.setLabelProvider(colDef.provider);
			tableColumnLayout.setColumnData(column.getColumn(),
					new ColumnWeightData(colDef.weight, colDef.minWidth, true));
		}

		tableCmp.setLayout(tableColumnLayout);
		return viewer;
	}

	@Override
	public void dispose() {
		super.dispose();
	}

	private class UsersContentProvider implements IStructuredContentProvider {
		private static final long serialVersionUID = 1L;

		public Object[] getElements(Object inputElement) {
			return (Object[]) inputElement;
		}

		public void dispose() {
		}

		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		}
	}

	// /* MANAGE FILTER */
	// private void createFilterPart(Composite parent) {
	// // Text Area for the filter
	// filterTxt = new Text(parent, SWT.BORDER | SWT.SEARCH | SWT.ICON_SEARCH
	// | SWT.ICON_CANCEL);
	// filterTxt.setMessage(filterHelpMsg);
	// filterTxt.setLayoutData(new GridData(GridData.GRAB_HORIZONTAL
	// | GridData.HORIZONTAL_ALIGN_FILL));
	// filterTxt.addModifyListener(new ModifyListener() {
	// private static final long serialVersionUID = 1L;
	//
	// public void modifyText(ModifyEvent event) {
	// refreshFilteredList();
	// }
	// });
	// }

	/**
	 * Refresh the user list: caller might overwrite in order to display a
	 * subset of all users, typically to remove current user from the list
	 */
	protected void refreshFilteredList(String filter) {
		try {
			Role[] roles = userAdmin.getRoles(filter);
			List<User> users = new ArrayList<User>();
			for (Role role : roles)
				if (role.getType() == Role.USER && role.getType() != Role.GROUP)
					users.add((User) role);
			usersViewer.setInput(users.toArray());
		} catch (InvalidSyntaxException e) {
			throw new ArgeoException("Unable to get roles with filter: "
					+ filter, e);
		}
	}

	// Local helpers

	private abstract class UseradminAbstractLP extends ColumnLabelProvider {
		private static final long serialVersionUID = 137336765024922368L;

		@Override
		public Font getFont(Object element) {
			// TODO manage fonts
			// // self
			// String username = getProperty(elem, ARGEO_USER_ID);
			// if (username.equals(session.getUserID()))
			// return bold;
			// // disabled
			// try {
			// Node userProfile = (Node) elem;
			// if (!userProfile.getProperty(ARGEO_ENABLED).getBoolean())
			// return italic;
			// else
			// return null;
			// } catch (RepositoryException e) {
			// throw new ArgeoException("Cannot get font for " + username, e);
			// }
			// }

			return super.getFont(element);
		}

		@Override
		public String getText(Object element) {
			User user = (User) element;
			return getText(user);
		}

		public abstract String getText(User user);
	}

	private class UserNameLP extends UseradminAbstractLP {
		private static final long serialVersionUID = 6550449442061090388L;

		@Override
		public String getText(User user) {
			return user.getName();
		}
	}

	private class CommonNameLP extends UseradminAbstractLP {
		private static final long serialVersionUID = 5256703081044911941L;

		@Override
		public String getText(User user) {
			Object obj = user.getProperties().get("cn");
			if (obj != null)
				return (String) obj;
			else
				return "";
		}
	}

	private class MailLP extends UseradminAbstractLP {
		private static final long serialVersionUID = 8329764452141982707L;

		@Override
		public String getText(User user) {
			Object obj = user.getProperties().get("mail");
			if (obj != null)
				return (String) obj;
			else
				return "";
		}
	}

	protected class ColumnDefinition {
		protected ColumnLabelProvider provider;
		protected String label;
		protected int weight;
		protected int minWidth;

		// public ColumnDefinition(ColumnLabelProvider provider, String label,
		// int weight, int minimumWidth) {
		// this.provider = provider;
		// this.label = label;
		// this.weight = weight;
		// this.minWidth = minimumWidth;
		// }

		public ColumnDefinition(ColumnLabelProvider provider, String label,
				int weight) {
			this.provider = provider;
			this.label = label;
			this.weight = weight;
			this.minWidth = weight;
		}
	}

	protected UserAdmin userAdmin() {
		return userAdmin;
	}

	/* DEPENDENCY INJECTION */
	public void setUserAdmin(UserAdmin userAdmin) {
		this.userAdmin = userAdmin;
	}
}