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
package org.argeo.cms.e4.users;

import static org.argeo.cms.util.UserAdminUtils.getProperty;
import static org.argeo.naming.LdapAttrs.cn;
import static org.argeo.naming.LdapAttrs.givenName;
import static org.argeo.naming.LdapAttrs.mail;
import static org.argeo.naming.LdapAttrs.sn;
import static org.argeo.naming.LdapAttrs.uid;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.inject.Inject;

import org.argeo.cms.auth.CurrentUser;
import org.argeo.cms.e4.users.providers.CommonNameLP;
import org.argeo.cms.e4.users.providers.DomainNameLP;
import org.argeo.cms.e4.users.providers.RoleIconLP;
import org.argeo.cms.e4.users.providers.UserFilter;
import org.argeo.cms.ui.eclipse.forms.AbstractFormPart;
//import org.argeo.cms.ui.eclipse.forms.FormToolkit;
import org.argeo.cms.ui.eclipse.forms.IManagedForm;
import org.argeo.cms.util.CmsUtils;
import org.argeo.cms.util.UserAdminUtils;
import org.argeo.eclipse.ui.ColumnDefinition;
import org.argeo.eclipse.ui.EclipseUiUtils;
import org.argeo.eclipse.ui.parts.LdifUsersTable;
import org.argeo.naming.LdapAttrs;
import org.argeo.node.NodeConstants;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.TrayDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerDropAdapter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.dnd.TransferData;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.osgi.service.useradmin.Group;
import org.osgi.service.useradmin.Role;
import org.osgi.service.useradmin.User;
import org.osgi.service.useradmin.UserAdmin;
import org.osgi.service.useradmin.UserAdminEvent;

/** Display/edit the properties of a given user */
public class UserEditor extends AbstractRoleEditor {
	// final static String ID = "UserEditor.mainPage";

	@Inject
	private EPartService partService;

	// private final UserEditor editor;
	// private UserAdminWrapper userAdminWrapper;

	// Local configuration
	// private final int PRE_TITLE_INDENT = 10;

	// public UserMainPage(FormEditor editor, UserAdminWrapper userAdminWrapper) {
	// super(editor, ID, "Main");
	// this.editor = (UserEditor) editor;
	// this.userAdminWrapper = userAdminWrapper;
	// }

	// protected void createFormContent(final IManagedForm mf) {
	// ScrolledForm form = mf.getForm();
	// Composite body = form.getBody();
	// GridLayout mainLayout = new GridLayout();
	// // mainLayout.marginRight = 10;
	// body.setLayout(mainLayout);
	// User user = editor.getDisplayedUser();
	// appendOverviewPart(body, user);
	// // Remove to ability to force the password for his own user. The user
	// // must then use the change pwd feature
	// appendMemberOfPart(body, user);
	// }

	@Override
	protected void createUi(Composite body) {
		// Composite body = new Composite(parent, SWT.BORDER);
		GridLayout mainLayout = new GridLayout();
		// mainLayout.marginRight = 10;
		body.setLayout(mainLayout);
		// body.getParent().setLayout(new GridLayout());
		// body.setLayoutData(CmsUtils.fillAll());
		User user = getDisplayedUser();
		appendOverviewPart(body, user);
		// Remove to ability to force the password for his own user. The user
		// must then use the change pwd feature
		appendMemberOfPart(body, user);
	}

	/** Creates the general section */
	private void appendOverviewPart(final Composite parent, final User user) {
		// FormToolkit tk = getManagedForm().getToolkit();

		// Section section = tk.createSection(parent, SWT.NO_FOCUS);
		// GridData gd = EclipseUiUtils.fillWidth();
		// // gd.verticalAlignment = PRE_TITLE_INDENT;
		// section.setLayoutData(gd);
		Composite body = new Composite(parent, SWT.NONE);
		body.setLayoutData(EclipseUiUtils.fillWidth());
		// section.setClient(body);
		// body.setLayout(new GridLayout(6, false));
		body.setLayout(new GridLayout(2, false));

		Text commonName = createReadOnlyLT(body, "Name", getProperty(user, cn));
		Text distinguishedName = createReadOnlyLT(body, "Login", getProperty(user, uid));
		Text firstName = createLT(body, "First name", getProperty(user, givenName));
		Text lastName = createLT(body, "Last name", getProperty(user, sn));
		Text email = createLT(body, "Email", getProperty(user, mail));

		Link resetPwdLk = new Link(body, SWT.NONE);
		if (!UserAdminUtils.isCurrentUser(user)) {
			resetPwdLk.setText("<a>Reset password</a>");
		}
		resetPwdLk.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));

		// create form part (controller)
		AbstractFormPart part = new AbstractFormPart() {
			private MainInfoListener listener;

			@Override
			public void initialize(IManagedForm form) {
				super.initialize(form);
				listener = new MainInfoListener(parent.getDisplay(), this);
				userAdminWrapper.addListener(listener);
			}

			@Override
			public void dispose() {
				userAdminWrapper.removeListener(listener);
				super.dispose();
			}

			@SuppressWarnings("unchecked")
			public void commit(boolean onSave) {
				// TODO Sanity checks (mail validity...)
				user.getProperties().put(LdapAttrs.givenName.name(), firstName.getText());
				user.getProperties().put(LdapAttrs.sn.name(), lastName.getText());
				user.getProperties().put(LdapAttrs.cn.name(), commonName.getText());
				user.getProperties().put(LdapAttrs.mail.name(), email.getText());
				super.commit(onSave);
			}

			@Override
			public void refresh() {
				distinguishedName.setText(UserAdminUtils.getProperty(user, LdapAttrs.uid.name()));
				commonName.setText(UserAdminUtils.getProperty(user, LdapAttrs.cn.name()));
				firstName.setText(UserAdminUtils.getProperty(user, LdapAttrs.givenName.name()));
				lastName.setText(UserAdminUtils.getProperty(user, LdapAttrs.sn.name()));
				email.setText(UserAdminUtils.getProperty(user, LdapAttrs.mail.name()));
				refreshFormTitle(user);
				super.refresh();
			}
		};

		// Improve this: automatically generate CN when first or last name
		// changes
		ModifyListener cnML = new ModifyListener() {
			private static final long serialVersionUID = 4298649222869835486L;

			@Override
			public void modifyText(ModifyEvent event) {
				String first = firstName.getText();
				String last = lastName.getText();
				String cn = first.trim() + " " + last.trim() + " ";
				cn = cn.trim();
				commonName.setText(cn);
				// getManagedForm().getForm().setText(cn);
				updateEditorTitle(cn);
			}
		};
		firstName.addModifyListener(cnML);
		lastName.addModifyListener(cnML);

		ModifyListener defaultListener = new FormPartML(part);
		firstName.addModifyListener(defaultListener);
		lastName.addModifyListener(defaultListener);
		email.addModifyListener(defaultListener);

		if (!UserAdminUtils.isCurrentUser(user))
			resetPwdLk.addSelectionListener(new SelectionAdapter() {
				private static final long serialVersionUID = 5881800534589073787L;

				@Override
				public void widgetSelected(SelectionEvent e) {
					new ChangePasswordDialog(user, "Reset password").open();
				}
			});

		getManagedForm().addPart(part);
	}

	private class ChangePasswordDialog extends TrayDialog {
		private static final long serialVersionUID = 2843538207460082349L;

		private User user;
		private Text password1;
		private Text password2;
		private String title;
		// private FormToolkit tk;

		public ChangePasswordDialog(User user, String title) {
			super(Display.getDefault().getActiveShell());
			// this.tk = tk;
			this.user = user;
			this.title = title;
		}

		protected Control createDialogArea(Composite parent) {
			Composite dialogarea = (Composite) super.createDialogArea(parent);
			dialogarea.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
			Composite body = new Composite(dialogarea, SWT.NO_FOCUS);
			body.setLayoutData(EclipseUiUtils.fillAll());
			GridLayout layout = new GridLayout(2, false);
			body.setLayout(layout);

			password1 = createLP(body, "New password", "");
			password2 = createLP(body, "Repeat password", "");
			parent.pack();
			return body;
		}

		@SuppressWarnings("unchecked")
		@Override
		protected void okPressed() {
			String msg = null;

			if (password1.getText().equals(""))
				msg = "Password cannot be empty";
			else if (password1.getText().equals(password2.getText())) {
				char[] newPassword = password1.getText().toCharArray();
				// userAdminWrapper.beginTransactionIfNeeded();
				userAdminWrapper.beginTransactionIfNeeded();
				user.getCredentials().put(null, newPassword);
				userAdminWrapper.commitOrNotifyTransactionStateChange();
				super.okPressed();
			} else {
				msg = "Passwords are not equals";
			}

			if (EclipseUiUtils.notEmpty(msg))
				MessageDialog.openError(getParentShell(), "Cannot reset pasword", msg);
		}

		protected void configureShell(Shell shell) {
			super.configureShell(shell);
			shell.setText(title);
		}
	}

	private LdifUsersTable appendMemberOfPart(final Composite parent, User user) {
		// Section section = addSection(tk, parent, "Roles");
		// Composite body = (Composite) section.getClient();
		// Composite body= parent;
		Composite body = new Composite(parent, SWT.BORDER);
		body.setLayout(new GridLayout());
		body.setLayoutData(CmsUtils.fillAll());

		// boolean isAdmin = CurrentUser.isInRole(NodeConstants.ROLE_ADMIN);

		// Displayed columns
		List<ColumnDefinition> columnDefs = new ArrayList<ColumnDefinition>();
		columnDefs.add(new ColumnDefinition(new RoleIconLP(), "", 0, 24));
		columnDefs.add(new ColumnDefinition(new CommonNameLP(), "Name", 150));
		columnDefs.add(new ColumnDefinition(new DomainNameLP(), "Domain", 100));
		// Only show technical DN to administrators
		// if (isAdmin)
		// columnDefs.add(new ColumnDefinition(new UserNameLP(), "Distinguished Name",
		// 300));

		// Create and configure the table
		final LdifUsersTable userViewerCmp = new MyUserTableViewer(body, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL, user);

		userViewerCmp.setColumnDefinitions(columnDefs);
		// if (isAdmin)
		// userViewerCmp.populateWithStaticFilters(false, false);
		// else
		userViewerCmp.populate(true, false);
		GridData gd = EclipseUiUtils.fillAll();
		gd.heightHint = 500;
		userViewerCmp.setLayoutData(gd);

		// Controllers
		TableViewer userViewer = userViewerCmp.getTableViewer();
		userViewer.addDoubleClickListener(new UserTableDefaultDClickListener(partService));
		int operations = DND.DROP_COPY | DND.DROP_MOVE;
		Transfer[] tt = new Transfer[] { TextTransfer.getInstance() };
		GroupDropListener dropL = new GroupDropListener(userAdminWrapper, userViewer, user);
		userViewer.addDropSupport(operations, tt, dropL);

		AbstractFormPart part = new AbstractFormPart() {

			private GroupChangeListener listener;

			@Override
			public void initialize(IManagedForm form) {
				super.initialize(form);
				listener = new GroupChangeListener(parent.getDisplay(), this);
				userAdminWrapper.addListener(listener);
			}

			public void commit(boolean onSave) {
				super.commit(onSave);
			}

			@Override
			public void dispose() {
				userAdminWrapper.removeListener(listener);
				super.dispose();
			}

			@Override
			public void refresh() {
				userViewerCmp.refresh();
				super.refresh();
			}
		};
		getManagedForm().addPart(part);
		// addRemoveAbitily(body, userViewer, user);
		// userViewerCmp.refresh();
		String tooltip = "Remove " + UserAdminUtils.getUserLocalId(user.getName()) + " from the below selected groups";
		Action action = new RemoveMembershipAction(userViewer, user, tooltip, SecurityAdminImages.ICON_REMOVE_DESC);
		ToolBarManager toolBarManager = new ToolBarManager(SWT.FLAT);
		ToolBar toolBar = toolBarManager.createControl(body);
		toolBar.setLayoutData(CmsUtils.fillWidth());
		toolBarManager.add(action);
		toolBarManager.update(true);
		return userViewerCmp;
	}

	private class MyUserTableViewer extends LdifUsersTable {
		private static final long serialVersionUID = 2653790051461237329L;

		private Button showSystemRoleBtn;

		private final User user;
		private final UserFilter userFilter;

		public MyUserTableViewer(Composite parent, int style, User user) {
			super(parent, style, true);
			this.user = user;
			userFilter = new UserFilter();
		}

		protected void populateStaticFilters(Composite staticFilterCmp) {
			staticFilterCmp.setLayout(new GridLayout());
			showSystemRoleBtn = new Button(staticFilterCmp, SWT.CHECK);
			showSystemRoleBtn.setText("Show system roles");
			boolean showSysRole = CurrentUser.isInRole(NodeConstants.ROLE_ADMIN);
			showSystemRoleBtn.setSelection(showSysRole);
			userFilter.setShowSystemRole(showSysRole);
			showSystemRoleBtn.addSelectionListener(new SelectionAdapter() {
				private static final long serialVersionUID = -7033424592697691676L;

				@Override
				public void widgetSelected(SelectionEvent e) {
					userFilter.setShowSystemRole(showSystemRoleBtn.getSelection());
					refresh();
				}
			});
		}

		@Override
		protected List<User> listFilteredElements(String filter) {
			List<User> users = (List<User>) getFlatGroups(null);
			List<User> filteredUsers = new ArrayList<User>();
			if (users.contains(user))
				users.remove(user);
			userFilter.setSearchText(filter);
			for (User user : users)
				if (userFilter.select(null, null, user))
					filteredUsers.add(user);
			return filteredUsers;
		}
	}

	// private void addRemoveAbility(Composite parent, TableViewer userViewer, User
	// user) {
	// // Section section = sectionPart.getSection();
	// ToolBarManager toolBarManager = new ToolBarManager(SWT.FLAT);
	// ToolBar toolbar = toolBarManager.createControl(parent);
	// final Cursor handCursor = new Cursor(Display.getCurrent(), SWT.CURSOR_HAND);
	// toolbar.setCursor(handCursor);
	// toolbar.addDisposeListener(new DisposeListener() {
	// private static final long serialVersionUID = 3882131405820522925L;
	//
	// public void widgetDisposed(DisposeEvent e) {
	// if ((handCursor != null) && (handCursor.isDisposed() == false)) {
	// handCursor.dispose();
	// }
	// }
	// });
	//
	// String tooltip = "Remove " + UserAdminUtils.getUserLocalId(user.getName()) +
	// " from the below selected groups";
	// Action action = new RemoveMembershipAction(userViewer, user, tooltip,
	// SecurityAdminImages.ICON_REMOVE_DESC);
	// toolBarManager.add(action);
	// toolBarManager.update(true);
	// // section.setTextClient(toolbar);
	// }

	private class RemoveMembershipAction extends Action {
		private static final long serialVersionUID = -1337713097184522588L;

		private final TableViewer userViewer;
		private final User user;

		RemoveMembershipAction(TableViewer userViewer, User user, String name, ImageDescriptor img) {
			super(name, img);
			this.userViewer = userViewer;
			this.user = user;
		}

		@Override
		public void run() {
			ISelection selection = userViewer.getSelection();
			if (selection.isEmpty())
				return;

			@SuppressWarnings("unchecked")
			Iterator<Group> it = ((IStructuredSelection) selection).iterator();
			List<Group> groups = new ArrayList<Group>();
			while (it.hasNext()) {
				Group currGroup = it.next();
				groups.add(currGroup);
			}

			userAdminWrapper.beginTransactionIfNeeded();
			for (Group group : groups) {
				group.removeMember(user);
			}
			userAdminWrapper.commitOrNotifyTransactionStateChange();
			for (Group group : groups) {
				userAdminWrapper.notifyListeners(new UserAdminEvent(null, UserAdminEvent.ROLE_CHANGED, group));
			}
		}
	}

	/**
	 * Defines the table as being a potential target to add group memberships
	 * (roles) to this user
	 */
	private class GroupDropListener extends ViewerDropAdapter {
		private static final long serialVersionUID = 2893468717831451621L;

		private final UserAdminWrapper myUserAdminWrapper;
		private final User myUser;

		public GroupDropListener(UserAdminWrapper userAdminWrapper, Viewer userViewer, User user) {
			super(userViewer);
			this.myUserAdminWrapper = userAdminWrapper;
			this.myUser = user;
		}

		@Override
		public boolean validateDrop(Object target, int operation, TransferData transferType) {
			// Target is always OK in a list only view
			// TODO check if not a string
			boolean validDrop = true;
			return validDrop;
		}

		@Override
		public void drop(DropTargetEvent event) {
			String name = (String) event.data;
			UserAdmin myUserAdmin = myUserAdminWrapper.getUserAdmin();
			Role role = myUserAdmin.getRole(name);
			// TODO this check should be done before.
			if (role.getType() == Role.GROUP) {
				// TODO check if the user is already member of this group

				myUserAdminWrapper.beginTransactionIfNeeded();
				Group group = (Group) role;
				group.addMember(myUser);
				userAdminWrapper.commitOrNotifyTransactionStateChange();
				myUserAdminWrapper.notifyListeners(new UserAdminEvent(null, UserAdminEvent.ROLE_CHANGED, group));
			}
			super.drop(event);
		}

		@Override
		public boolean performDrop(Object data) {
			// userTableViewerCmp.refresh();
			return true;
		}
	}

	// LOCAL HELPERS
	private void refreshFormTitle(User group) {
		// getManagedForm().getForm().setText(UserAdminUtils.getProperty(group,
		// LdapAttrs.cn.name()));
	}

	/** Appends a section with a title */
	// private Section addSection(FormToolkit tk, Composite parent, String title) {
	// Section section = tk.createSection(parent, Section.TITLE_BAR);
	// GridData gd = EclipseUiUtils.fillWidth();
	// gd.verticalAlignment = PRE_TITLE_INDENT;
	// section.setLayoutData(gd);
	// section.setText(title);
	// // section.getMenu().setVisible(true);
	//
	// Composite body = tk.createComposite(section, SWT.WRAP);
	// body.setLayoutData(EclipseUiUtils.fillAll());
	// section.setClient(body);
	//
	// return section;
	// }

}
