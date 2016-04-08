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
import java.util.Iterator;
import java.util.List;

import org.argeo.ArgeoException;
import org.argeo.cms.auth.AuthConstants;
import org.argeo.cms.util.useradmin.UserAdminUtils;
import org.argeo.eclipse.ui.ColumnDefinition;
import org.argeo.eclipse.ui.EclipseUiUtils;
import org.argeo.eclipse.ui.parts.LdifUsersTable;
import org.argeo.jcr.ArgeoNames;
import org.argeo.osgi.useradmin.LdifName;
import org.argeo.security.ui.admin.SecurityAdminImages;
import org.argeo.security.ui.admin.internal.UserAdminWrapper;
import org.argeo.security.ui.admin.internal.parts.UserEditor.GroupChangeListener;
import org.argeo.security.ui.admin.internal.parts.UserEditor.MainInfoListener;
import org.argeo.security.ui.admin.internal.providers.CommonNameLP;
import org.argeo.security.ui.admin.internal.providers.DomainNameLP;
import org.argeo.security.ui.admin.internal.providers.RoleIconLP;
import org.argeo.security.ui.admin.internal.providers.UserFilter;
import org.argeo.security.ui.admin.internal.providers.UserNameLP;
import org.argeo.security.ui.admin.internal.providers.UserTableDefaultDClickListener;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ToolBarManager;
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
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.ui.forms.AbstractFormPart;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.SectionPart;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;
import org.osgi.service.useradmin.Group;
import org.osgi.service.useradmin.Role;
import org.osgi.service.useradmin.User;
import org.osgi.service.useradmin.UserAdmin;
import org.osgi.service.useradmin.UserAdminEvent;

/** Display/edit the properties of a given user */
public class UserMainPage extends FormPage implements ArgeoNames {
	final static String ID = "UserEditor.mainPage";

	private final UserEditor editor;
	private UserAdminWrapper userAdminWrapper;

	// Local configuration
	private final int PRE_TITLE_INDENT = 10;

	public UserMainPage(FormEditor editor, UserAdminWrapper userAdminWrapper) {
		super(editor, ID, "Main");
		this.editor = (UserEditor) editor;
		this.userAdminWrapper = userAdminWrapper;
	}

	protected void createFormContent(final IManagedForm mf) {
		ScrolledForm form = mf.getForm();
		Composite body = form.getBody();
		GridLayout mainLayout = new GridLayout();
		// mainLayout.marginRight = 10;
		body.setLayout(mainLayout);
		User user = editor.getDisplayedUser();
		appendOverviewPart(body, user);
		// Remove to ability to force the password for his own user. The user
		// must then use the change pwd feature
		if (!UserAdminUtils.isCurrentUser(user))
			appendPasswordPart(body, user);
		appendMemberOfPart(body, user);
	}

	/** Creates the general section */
	private void appendOverviewPart(Composite parent, final User user) {
		FormToolkit tk = getManagedForm().getToolkit();

		Section section = addSection(tk, parent, "Main information");
		Composite body = (Composite) section.getClient();
		body.setLayout(new GridLayout(2, false));

		final Text distinguishedName = createLT(tk, body, "User Name",
				UserAdminUtils.getProperty(user, LdifName.uid.name()));
		distinguishedName.setEnabled(false);

		final Text commonName = createLT(tk, body, "Common Name",
				UserAdminUtils.getProperty(user, LdifName.cn.name()));
		commonName.setEnabled(false);

		final Text firstName = createLT(tk, body, "First name",
				UserAdminUtils.getProperty(user, LdifName.givenName.name()));

		final Text lastName = createLT(tk, body, "Last name",
				UserAdminUtils.getProperty(user, LdifName.sn.name()));

		final Text email = createLT(tk, body, "Email",
				UserAdminUtils.getProperty(user, LdifName.mail.name()));

		// create form part (controller)
		AbstractFormPart part = new SectionPart((Section) body.getParent()) {
			private MainInfoListener listener;

			@Override
			public void initialize(IManagedForm form) {
				super.initialize(form);
				listener = editor.new MainInfoListener(parent.getDisplay(),
						this);
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
				user.getProperties().put(LdifName.givenName.name(),
						firstName.getText());
				user.getProperties()
						.put(LdifName.sn.name(), lastName.getText());
				user.getProperties().put(LdifName.cn.name(),
						commonName.getText());
				user.getProperties().put(LdifName.mail.name(), email.getText());
				super.commit(onSave);
			}

			@Override
			public void refresh() {
				distinguishedName.setText(UserAdminUtils.getProperty(user,
						LdifName.uid.name()));
				commonName.setText(UserAdminUtils.getProperty(user,
						LdifName.cn.name()));
				firstName.setText(UserAdminUtils.getProperty(user,
						LdifName.givenName.name()));
				lastName.setText(UserAdminUtils.getProperty(user,
						LdifName.sn.name()));
				email.setText(UserAdminUtils.getProperty(user,
						LdifName.mail.name()));
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
				getManagedForm().getForm().setText(cn);
				editor.updateEditorTitle(cn);
			}
		};
		firstName.addModifyListener(cnML);
		lastName.addModifyListener(cnML);

		ModifyListener defaultListener = editor.new FormPartML(part);
		firstName.addModifyListener(defaultListener);
		lastName.addModifyListener(defaultListener);
		email.addModifyListener(defaultListener);
		getManagedForm().addPart(part);
	}

	/** Creates the password section */
	private void appendPasswordPart(Composite parent, final User user) {
		FormToolkit tk = getManagedForm().getToolkit();
		Section section = addSection(tk, parent, "Password");
		Composite body = (Composite) section.getClient();
		body.setLayout(new GridLayout(2, false));

		// add widgets (view)
		final Text password1 = createLP(tk, body, "New password", "");
		final Text password2 = createLP(tk, body, "Repeat password", "");

		// create form part (controller)
		AbstractFormPart part = new SectionPart((Section) body.getParent()) {
			@SuppressWarnings("unchecked")
			public void commit(boolean onSave) {
				if (!password1.getText().equals("")
						|| !password2.getText().equals("")) {
					if (password1.getText().equals(password2.getText())) {
						char[] newPassword = password1.getText().toCharArray();
						userAdminWrapper.beginTransactionIfNeeded();
						user.getCredentials().put(null, newPassword);
						password1.setText("");
						password2.setText("");
						super.commit(onSave);
					} else {
						password1.setText("");
						password2.setText("");
						throw new ArgeoException("Passwords are not equals");
					}
				}
			}
		};
		ModifyListener defaultListener = editor.new FormPartML(part);
		password1.addModifyListener(defaultListener);
		password2.addModifyListener(defaultListener);
		getManagedForm().addPart(part);
	}

	private LdifUsersTable appendMemberOfPart(Composite parent, User user) {
		FormToolkit tk = getManagedForm().getToolkit();
		Section section = addSection(tk, parent, "Roles");
		Composite body = (Composite) section.getClient();
		body.setLayout(EclipseUiUtils.noSpaceGridLayout());

		boolean isAdmin = UserAdminUtils.isUserInRole(AuthConstants.ROLE_ADMIN);

		// Displayed columns
		List<ColumnDefinition> columnDefs = new ArrayList<ColumnDefinition>();
		columnDefs.add(new ColumnDefinition(new RoleIconLP(), "", 0, 24));
		columnDefs.add(new ColumnDefinition(new CommonNameLP(), "Common Name",
				150));
		columnDefs.add(new ColumnDefinition(new DomainNameLP(), "Domain Name",
				200));
		// Only show technical DN to administrators
		if (isAdmin)
			columnDefs.add(new ColumnDefinition(new UserNameLP(),
					"Distinguished Name", 120));

		// Create and configure the table
		final LdifUsersTable userViewerCmp = new MyUserTableViewer(body,
				SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL, user);

		userViewerCmp.setColumnDefinitions(columnDefs);
		if (isAdmin)
			userViewerCmp.populateWithStaticFilters(false, false);
		else
			userViewerCmp.populate(true, false);
		GridData gd = EclipseUiUtils.fillAll();
		gd.heightHint = 300;
		userViewerCmp.setLayoutData(gd);

		// Controllers
		TableViewer userViewer = userViewerCmp.getTableViewer();
		userViewer.addDoubleClickListener(new UserTableDefaultDClickListener());
		int operations = DND.DROP_COPY | DND.DROP_MOVE;
		Transfer[] tt = new Transfer[] { TextTransfer.getInstance() };
		GroupDropListener dropL = new GroupDropListener(userAdminWrapper,
				userViewer, user);
		userViewer.addDropSupport(operations, tt, dropL);

		SectionPart part = new SectionPart((Section) body.getParent()) {

			private GroupChangeListener listener;

			@Override
			public void initialize(IManagedForm form) {
				super.initialize(form);
				listener = editor.new GroupChangeListener(parent.getDisplay(),
						this);
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
		addRemoveAbitily(part, userViewer, user);
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
			userFilter.setShowSystemRole(false);
		}

		protected void populateStaticFilters(Composite staticFilterCmp) {
			staticFilterCmp.setLayout(new GridLayout());
			showSystemRoleBtn = new Button(staticFilterCmp, SWT.CHECK);
			showSystemRoleBtn.setText("Show system roles");
			showSystemRoleBtn.addSelectionListener(new SelectionAdapter() {
				private static final long serialVersionUID = -7033424592697691676L;

				@Override
				public void widgetSelected(SelectionEvent e) {
					userFilter.setShowSystemRole(showSystemRoleBtn
							.getSelection());
					refresh();
				}
			});
		}

		@Override
		protected List<User> listFilteredElements(String filter) {
			List<User> users = (List<User>) editor.getFlatGroups(null);
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

	private void addRemoveAbitily(SectionPart sectionPart,
			TableViewer userViewer, User user) {
		Section section = sectionPart.getSection();
		ToolBarManager toolBarManager = new ToolBarManager(SWT.FLAT);
		ToolBar toolbar = toolBarManager.createControl(section);
		final Cursor handCursor = new Cursor(section.getDisplay(),
				SWT.CURSOR_HAND);
		toolbar.setCursor(handCursor);
		toolbar.addDisposeListener(new DisposeListener() {
			private static final long serialVersionUID = 3882131405820522925L;

			public void widgetDisposed(DisposeEvent e) {
				if ((handCursor != null) && (handCursor.isDisposed() == false)) {
					handCursor.dispose();
				}
			}
		});

		String tooltip = "Remove " + UserAdminUtils.getUsername(user)
				+ " from the below selected groups";
		Action action = new RemoveMembershipAction(userViewer, user, tooltip,
				SecurityAdminImages.ICON_REMOVE_DESC);
		toolBarManager.add(action);
		toolBarManager.update(true);
		section.setTextClient(toolbar);
	}

	private class RemoveMembershipAction extends Action {
		private static final long serialVersionUID = -1337713097184522588L;

		private final TableViewer userViewer;
		private final User user;

		RemoveMembershipAction(TableViewer userViewer, User user, String name,
				ImageDescriptor img) {
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
			// StringBuilder builder = new StringBuilder();
			while (it.hasNext()) {
				Group currGroup = it.next();
				// String groupName = UserAdminUtils.getUsername(currGroup);
				// builder.append(groupName).append("; ");
				groups.add(currGroup);
			}

			// if (!MessageDialog.openQuestion(
			// HandlerUtil.getActiveShell(event),
			// "Re",
			// "Are you sure that you want to delete these users?\n"
			// + builder.substring(0, builder.length() - 2)))
			// return null;

			userAdminWrapper.beginTransactionIfNeeded();
			for (Group group : groups) {
				group.removeMember(user);
				// sectionPart.refresh();
				userAdminWrapper.notifyListeners(new UserAdminEvent(null,
						UserAdminEvent.ROLE_CHANGED, group));
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

		public GroupDropListener(UserAdminWrapper userAdminWrapper,
				Viewer userViewer, User user) {
			super(userViewer);
			this.myUserAdminWrapper = userAdminWrapper;
			this.myUser = user;
		}

		@Override
		public boolean validateDrop(Object target, int operation,
				TransferData transferType) {
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
				myUserAdminWrapper.notifyListeners(new UserAdminEvent(null,
						UserAdminEvent.ROLE_CHANGED, group));
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
		getManagedForm().getForm().setText(
				UserAdminUtils.getProperty(group, LdifName.cn.name()));
	}

	/** Appends a section with a title */
	private Section addSection(FormToolkit tk, Composite parent, String title) {
		Section section = tk.createSection(parent, Section.TITLE_BAR);
		GridData gd = EclipseUiUtils.fillWidth();
		gd.verticalAlignment = PRE_TITLE_INDENT;
		section.setLayoutData(gd);
		section.setText(title);
		// section.getMenu().setVisible(true);

		Composite body = tk.createComposite(section, SWT.WRAP);
		body.setLayoutData(EclipseUiUtils.fillAll());
		section.setClient(body);

		return section;
	}

	/** Creates label and multiline text. */
	Text createLMT(FormToolkit toolkit, Composite body, String label,
			String value) {
		Label lbl = toolkit.createLabel(body, label);
		lbl.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		Text text = toolkit.createText(body, value, SWT.BORDER | SWT.MULTI);
		text.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true));
		return text;
	}

	/** Creates label and password. */
	Text createLP(FormToolkit toolkit, Composite body, String label,
			String value) {
		Label lbl = toolkit.createLabel(body, label);
		lbl.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		Text text = toolkit.createText(body, value, SWT.BORDER | SWT.PASSWORD);
		text.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		return text;
	}

	/** Creates label and text. */
	Text createLT(FormToolkit toolkit, Composite body, String label,
			String value) {
		Label lbl = toolkit.createLabel(body, label);
		lbl.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		Text text = toolkit.createText(body, value, SWT.BORDER);
		text.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		return text;
	}
}