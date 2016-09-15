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

import javax.transaction.UserTransaction;

import org.argeo.cms.CmsException;
import org.argeo.cms.util.useradmin.UserAdminUtils;
import org.argeo.eclipse.ui.ColumnDefinition;
import org.argeo.eclipse.ui.EclipseUiUtils;
import org.argeo.eclipse.ui.parts.LdifUsersTable;
import org.argeo.node.ArgeoNames;
import org.argeo.osgi.useradmin.LdifName;
import org.argeo.security.ui.admin.SecurityAdminImages;
import org.argeo.security.ui.admin.internal.UserAdminWrapper;
import org.argeo.security.ui.admin.internal.parts.UserEditor.GroupChangeListener;
import org.argeo.security.ui.admin.internal.parts.UserEditor.MainInfoListener;
import org.argeo.security.ui.admin.internal.providers.CommonNameLP;
import org.argeo.security.ui.admin.internal.providers.MailLP;
import org.argeo.security.ui.admin.internal.providers.RoleIconLP;
import org.argeo.security.ui.admin.internal.providers.UserFilter;
import org.argeo.security.ui.admin.internal.providers.UserNameLP;
import org.argeo.security.ui.admin.internal.providers.UserTableDefaultDClickListener;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.ViewerDropAdapter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.dnd.TransferData;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
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

/** Display/edit main properties of a given group */
public class GroupMainPage extends FormPage implements ArgeoNames {
	final static String ID = "GroupEditor.mainPage";

	private final UserEditor editor;
	private UserAdminWrapper userAdminWrapper;

	// Local configuration
	private final int PRE_TITLE_INDENT = 10;

	public GroupMainPage(FormEditor editor, UserAdminWrapper userAdminWrapper) {
		super(editor, ID, "Main");
		this.editor = (UserEditor) editor;
		this.userAdminWrapper = userAdminWrapper;
	}

	protected void createFormContent(final IManagedForm mf) {
		ScrolledForm form = mf.getForm();
		Composite body = form.getBody();
		GridLayout mainLayout = new GridLayout();
		body.setLayout(mainLayout);
		Group group = (Group) editor.getDisplayedUser();
		appendOverviewPart(body, group);
		appendMembersPart(body, group);
	}

	/** Creates the general section */
	protected void appendOverviewPart(final Composite parent, final Group group) {
		FormToolkit tk = getManagedForm().getToolkit();
		Composite body = addSection(tk, parent, "Main information");
		GridLayout layout = new GridLayout(2, false);
		body.setLayout(layout);

		final Text dnTxt = createLT(body, "DN", group.getName());
		dnTxt.setEnabled(false);

		final Text cnTxt = createLT(body, "Common Name",
				UserAdminUtils.getProperty(group, LdifName.cn.name()));
		cnTxt.setEnabled(false);

		Label descLbl = new Label(body, SWT.LEAD);
		descLbl.setText("Description");
		descLbl.setLayoutData(new GridData(SWT.RIGHT, SWT.TOP, false, false));
		final Text descTxt = new Text(body, SWT.LEAD | SWT.MULTI | SWT.WRAP
				| SWT.BORDER);
		GridData gd = EclipseUiUtils.fillAll();
		gd.heightHint = 100;
		descTxt.setLayoutData(gd);

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
				group.getProperties().put(LdifName.description.name(),
						descTxt.getText());
				// Enable common name ?
				// editor.setProperty(UserAdminConstants.KEY_CN,
				// email.getText());
				super.commit(onSave);
			}

			@Override
			public void refresh() {
				refreshFormTitle(group);
				dnTxt.setText(group.getName());
				cnTxt.setText(UserAdminUtils.getProperty(group,
						LdifName.cn.name()));
				descTxt.setText(UserAdminUtils.getProperty(group,
						LdifName.description.name()));
				super.refresh();
			}
		};

		ModifyListener defaultListener = editor.new FormPartML(part);
		descTxt.addModifyListener(defaultListener);
		getManagedForm().addPart(part);
	}

	/** Filtered table with members. Has drag & drop ability */
	protected void appendMembersPart(Composite parent, Group group) {

		FormToolkit tk = getManagedForm().getToolkit();
		Section section = tk.createSection(parent, Section.TITLE_BAR);
		section.setLayoutData(EclipseUiUtils.fillAll());

		Composite body = new Composite(section, SWT.NO_FOCUS);
		section.setClient(body);
		body.setLayoutData(EclipseUiUtils.fillAll());

		LdifUsersTable userTableViewerCmp = createMemberPart(body, group);

		SectionPart part = new GroupMembersPart(section, userTableViewerCmp,
				group);
		getManagedForm().addPart(part);
		addRemoveAbitily(part, userTableViewerCmp.getTableViewer(), group);
	}

	public LdifUsersTable createMemberPart(Composite parent, Group group) {
		parent.setLayout(EclipseUiUtils.noSpaceGridLayout());

		// Define the displayed columns
		List<ColumnDefinition> columnDefs = new ArrayList<ColumnDefinition>();
		columnDefs.add(new ColumnDefinition(new RoleIconLP(), "", 0, 24));
		columnDefs.add(new ColumnDefinition(new CommonNameLP(), "Common Name",
				150));
		columnDefs.add(new ColumnDefinition(new MailLP(), "Primary Mail", 150));
		columnDefs.add(new ColumnDefinition(new UserNameLP(),
				"Distinguished Name", 240));

		// Create and configure the table
		LdifUsersTable userViewerCmp = new MyUserTableViewer(parent, SWT.MULTI
				| SWT.H_SCROLL | SWT.V_SCROLL, userAdminWrapper.getUserAdmin());

		userViewerCmp.setColumnDefinitions(columnDefs);
		userViewerCmp.populate(true, false);
		userViewerCmp.setLayoutData(EclipseUiUtils.fillAll());

		// Controllers
		TableViewer userViewer = userViewerCmp.getTableViewer();
		userViewer.addDoubleClickListener(new UserTableDefaultDClickListener());
		int operations = DND.DROP_COPY | DND.DROP_MOVE;
		Transfer[] tt = new Transfer[] { TextTransfer.getInstance() };
		userViewer.addDropSupport(operations, tt,
				new GroupDropListener(userAdminWrapper, userViewerCmp,
						(Group) editor.getDisplayedUser()));

		return userViewerCmp;
	}

	// Local viewers
	private class MyUserTableViewer extends LdifUsersTable {
		private static final long serialVersionUID = 8467999509931900367L;

		private final UserFilter userFilter;

		public MyUserTableViewer(Composite parent, int style,
				UserAdmin userAdmin) {
			super(parent, style, true);
			userFilter = new UserFilter();

		}

		@Override
		protected List<User> listFilteredElements(String filter) {
			Group group = (Group) editor.getDisplayedUser();
			Role[] roles = group.getMembers();
			List<User> users = new ArrayList<User>();
			userFilter.setSearchText(filter);
			for (Role role : roles)
				// if (role.getType() == Role.GROUP)
				if (userFilter.select(null, null, role))
					users.add((User) role);
			return users;
		}
	}

	private void addRemoveAbitily(SectionPart sectionPart,
			TableViewer userViewer, Group group) {
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

		Action action = new RemoveMembershipAction(userViewer, group,
				"Remove selected items from this group",
				SecurityAdminImages.ICON_REMOVE_DESC);
		toolBarManager.add(action);
		toolBarManager.update(true);
		section.setTextClient(toolbar);
	}

	private class RemoveMembershipAction extends Action {
		private static final long serialVersionUID = -1337713097184522588L;

		private final TableViewer userViewer;
		private final Group group;

		RemoveMembershipAction(TableViewer userViewer, Group group,
				String name, ImageDescriptor img) {
			super(name, img);
			this.userViewer = userViewer;
			this.group = group;
		}

		@Override
		public void run() {
			ISelection selection = userViewer.getSelection();
			if (selection.isEmpty())
				return;

			@SuppressWarnings("unchecked")
			Iterator<User> it = ((IStructuredSelection) selection).iterator();
			List<User> users = new ArrayList<User>();
			while (it.hasNext()) {
				User currUser = it.next();
				users.add(currUser);
			}

			userAdminWrapper.beginTransactionIfNeeded();
			for (User user : users) {
				group.removeMember(user);
			}
			userAdminWrapper.commitOrNotifyTransactionStateChange();
			userAdminWrapper.notifyListeners(new UserAdminEvent(null,
					UserAdminEvent.ROLE_CHANGED, group));
		}
	}

	// LOCAL CONTROLLERS
	private class GroupMembersPart extends SectionPart {
		private final LdifUsersTable userViewer;
		private final Group group;

		private GroupChangeListener listener;

		public GroupMembersPart(Section section, LdifUsersTable userViewer,
				Group group) {
			super(section);
			this.userViewer = userViewer;
			this.group = group;
		}

		@Override
		public void initialize(IManagedForm form) {
			super.initialize(form);
			listener = editor.new GroupChangeListener(userViewer.getDisplay(),
					GroupMembersPart.this);
			userAdminWrapper.addListener(listener);
		}

		@Override
		public void dispose() {
			userAdminWrapper.removeListener(listener);
			super.dispose();
		}

		@Override
		public void refresh() {
			refreshFormTitle(group);
			getSection().setText(
					"Members of group "
							+ UserAdminUtils.getProperty(group,
									LdifName.cn.name()));
			userViewer.refresh();
			super.refresh();
		}
	}

	/**
	 * Defines this table as being a potential target to add group membership
	 * (roles) to this group
	 */
	private class GroupDropListener extends ViewerDropAdapter {
		private static final long serialVersionUID = 2893468717831451621L;

		private final UserAdminWrapper userAdminWrapper;
		// private final LdifUsersTable myUserViewerCmp;
		private final Group myGroup;

		public GroupDropListener(UserAdminWrapper userAdminWrapper,
				LdifUsersTable userTableViewerCmp, Group group) {
			super(userTableViewerCmp.getTableViewer());
			this.userAdminWrapper = userAdminWrapper;
			this.myGroup = group;
			// this.myUserViewerCmp = userTableViewerCmp;
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
			// TODO Is there an opportunity to perform the check before?

			String newUserName = (String) event.data;
			UserAdmin myUserAdmin = userAdminWrapper.getUserAdmin();
			Role role = myUserAdmin.getRole(newUserName);
			if (role.getType() == Role.GROUP) {
				Group newGroup = (Group) role;
				Shell shell = getViewer().getControl().getShell();
				// Sanity checks
				if (myGroup == newGroup) { // Equality
					MessageDialog.openError(shell, "Forbidden addition ",
							"A group cannot be a member of itself.");
					return;
				}

				// Cycle
				String myName = myGroup.getName();
				List<User> myMemberships = editor.getFlatGroups(myGroup);
				if (myMemberships.contains(newGroup)) {
					MessageDialog.openError(shell, "Forbidden addition: cycle",
							"Cannot add " + newUserName + " to group " + myName
									+ ". This would create a cycle");
					return;
				}

				// Already member
				List<User> newGroupMemberships = editor.getFlatGroups(newGroup);
				if (newGroupMemberships.contains(myGroup)) {
					MessageDialog.openError(shell, "Forbidden addition",
							"Cannot add " + newUserName + " to group " + myName
									+ ", this membership already exists");
					return;
				}
				userAdminWrapper.beginTransactionIfNeeded();
				myGroup.addMember(newGroup);
				userAdminWrapper.commitOrNotifyTransactionStateChange();
				userAdminWrapper.notifyListeners(new UserAdminEvent(null,
						UserAdminEvent.ROLE_CHANGED, myGroup));
			} else if (role.getType() == Role.USER) {
				// TODO check if the group is already member of this group
				UserTransaction transaction = userAdminWrapper
						.beginTransactionIfNeeded();
				User user = (User) role;
				myGroup.addMember(user);
				if (UserAdminWrapper.COMMIT_ON_SAVE)
					try {
						transaction.commit();
					} catch (Exception e) {
						throw new CmsException("Cannot commit transaction "
								+ "after user group membership update", e);
					}
				userAdminWrapper.notifyListeners(new UserAdminEvent(null,
						UserAdminEvent.ROLE_CHANGED, myGroup));
			}
			super.drop(event);
		}

		@Override
		public boolean performDrop(Object data) {
			// myUserViewerCmp.refresh();
			return true;
		}
	}

	// LOCAL HELPERS
	private void refreshFormTitle(Group group) {
		getManagedForm().getForm().setText(
				UserAdminUtils.getProperty(group, LdifName.cn.name()));
	}

	private Composite addSection(FormToolkit tk, Composite parent, String title) {
		Section section = tk.createSection(parent, Section.TITLE_BAR);
		GridData gd = EclipseUiUtils.fillWidth();
		gd.verticalAlignment = PRE_TITLE_INDENT;
		section.setLayoutData(gd);
		section.setText(title);
		Composite body = tk.createComposite(section, SWT.WRAP);
		body.setLayoutData(EclipseUiUtils.fillAll());
		section.setClient(body);
		return body;
	}

	/** Creates label and text. */
	private Text createLT(Composite body, String label, String value) {
		FormToolkit toolkit = getManagedForm().getToolkit();
		Label lbl = toolkit.createLabel(body, label);
		lbl.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		Text text = toolkit.createText(body, value, SWT.BORDER);
		text.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		return text;
	}
}