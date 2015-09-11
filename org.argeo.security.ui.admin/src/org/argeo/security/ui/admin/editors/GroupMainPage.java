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
package org.argeo.security.ui.admin.editors;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.RepositoryException;

import org.argeo.ArgeoException;
import org.argeo.eclipse.ui.EclipseUiUtils;
import org.argeo.jcr.ArgeoNames;
import org.argeo.security.ui.admin.internal.ColumnDefinition;
import org.argeo.security.ui.admin.internal.CommonNameLP;
import org.argeo.security.ui.admin.internal.MailLP;
import org.argeo.security.ui.admin.internal.RoleIconLP;
import org.argeo.security.ui.admin.internal.UserAdminConstants;
import org.argeo.security.ui.admin.internal.UserNameLP;
import org.argeo.security.ui.admin.internal.UserTableDefaultDClickListener;
import org.argeo.security.ui.admin.internal.UserTableViewer;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
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

/** Display/edit main properties of a given group */
public class GroupMainPage extends FormPage implements ArgeoNames {
	final static String ID = "GroupEditor.mainPage";

	private final UserEditor editor;
	private UserAdmin userAdmin;

	// Local configuration
	private final int PRE_TITLE_INDENT = 10;

	public GroupMainPage(FormEditor editor, UserAdmin userAdmin) {
		super(editor, ID, "Main");
		this.editor = (UserEditor) editor;
		this.userAdmin = userAdmin;
	}

	protected void createFormContent(final IManagedForm mf) {
		try {
			ScrolledForm form = mf.getForm();
			refreshFormTitle();

			// Body
			Composite body = form.getBody();
			GridLayout mainLayout = new GridLayout();
			body.setLayout(mainLayout);
			appendOverviewPart(body);
			appendMembersPart(body);
		} catch (RepositoryException e) {
			throw new ArgeoException("Cannot create form content", e);
		}
	}

	/** Creates the general section */
	protected void appendOverviewPart(Composite parent) {
		FormToolkit tk = getManagedForm().getToolkit();
		Composite body = addSection(tk, parent, "Main information");
		GridLayout layout = new GridLayout(2, false);
		body.setLayout(layout);

		Text distinguishedName = createLT(body, "Group Name",
				editor.getProperty(UserAdminConstants.KEY_UID));
		distinguishedName.setEnabled(false);

		final Text commonName = createLT(body, "Common Name",
				editor.getProperty(UserAdminConstants.KEY_CN));
		commonName.setEnabled(false);

		// create form part (controller)
		AbstractFormPart part = new SectionPart((Section) body.getParent()) {
			public void commit(boolean onSave) {
				super.commit(onSave);
			}
		};
		getManagedForm().addPart(part);
	}

	/** Filtered table with members. Has drag & drop ability */
	protected void appendMembersPart(Composite parent)
			throws RepositoryException {

		FormToolkit tk = getManagedForm().getToolkit();
		Section section = tk.createSection(parent, Section.TITLE_BAR);
		section.setLayoutData(EclipseUiUtils.fillAll());
		section.setText("Members of group "
				+ editor.getProperty(UserAdminConstants.KEY_CN));

		// Composite body = tk.createComposite(section, SWT.NONE);
		Composite body = new Composite(section, SWT.NO_FOCUS);
		section.setClient(body);
		body.setLayoutData(EclipseUiUtils.fillAll());

		createMemberPart(body);

		// create form part (controller)
		AbstractFormPart part = new SectionPart(section) {
			public void commit(boolean onSave) {
				super.commit(onSave);
			}
		};

		getManagedForm().addPart(part);
	}

	// UI Objects
	private UserTableViewer userTableViewerCmp;
	private TableViewer userViewer;
	private List<ColumnDefinition> columnDefs = new ArrayList<ColumnDefinition>();

	public void createMemberPart(Composite parent) {
		// parent.setLayout(EclipseUiUtils.noSpaceGridLayout());
		// parent2.setLayoutData(EclipseUiUtils.fillAll());
		// Composite parent = new Composite(parent2, SWT.NO_FOCUS);
		// parent.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		// parent.setLayoutData(EclipseUiUtils.fillAll());

		parent.setLayout(EclipseUiUtils.noSpaceGridLayout());
		// Define the displayed columns
		columnDefs.add(new ColumnDefinition(new RoleIconLP(), "", 0, 24));
		columnDefs.add(new ColumnDefinition(new UserNameLP(),
				"Distinguished Name", 240));
		columnDefs.add(new ColumnDefinition(new CommonNameLP(), "Common Name",
				150));
		columnDefs.add(new ColumnDefinition(new MailLP(), "Primary Mail", 150));

		// Create and configure the table
		userTableViewerCmp = new MyUserTableViewer(parent, SWT.MULTI
				| SWT.H_SCROLL | SWT.V_SCROLL, userAdmin);

		userTableViewerCmp.setColumnDefinitions(columnDefs);
		userTableViewerCmp.populate(true, false);
		// userTableViewerCmp.setLayoutData(new GridData(SWT.FILL, SWT.FILL,
		// false, false));
		userTableViewerCmp.setLayoutData(EclipseUiUtils.fillAll());

		// Links
		userViewer = userTableViewerCmp.getTableViewer();
		userViewer.addDoubleClickListener(new UserTableDefaultDClickListener());
		// Really?
		userTableViewerCmp.refresh();
	}

	private class MyUserTableViewer extends UserTableViewer {
		private static final long serialVersionUID = 8467999509931900367L;

		public MyUserTableViewer(Composite parent, int style,
				UserAdmin userAdmin) {
			super(parent, style, userAdmin, true);
		}

		@Override
		protected List<User> listFilteredElements(String filter) {
			Group group = (Group) editor.getDisplayedUser();
			Role[] roles = group.getMembers();
			List<User> users = new ArrayList<User>();
			for (Role role : roles)
				// if (role.getType() == Role.GROUP)
				users.add((User) role);
			return users;
		}
	}

	private void refreshFormTitle() throws RepositoryException {
		getManagedForm().getForm().setText(
				editor.getProperty(UserAdminConstants.KEY_CN));
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
	protected Text createLT(Composite body, String label, String value) {
		FormToolkit toolkit = getManagedForm().getToolkit();
		Label lbl = toolkit.createLabel(body, label);
		lbl.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		Text text = toolkit.createText(body, value, SWT.BORDER);
		text.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		return text;
	}

	// private class FormPartML implements ModifyListener {
	// private static final long serialVersionUID = 6299808129505381333L;
	// private AbstractFormPart formPart;
	//
	// public FormPartML(AbstractFormPart generalPart) {
	// this.formPart = generalPart;
	// }
	//
	// public void modifyText(ModifyEvent e) {
	// formPart.markDirty();
	// }
	// }
}