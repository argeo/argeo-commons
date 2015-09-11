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
import java.util.Arrays;
import java.util.List;

import org.argeo.ArgeoException;
import org.argeo.eclipse.ui.EclipseUiUtils;
import org.argeo.jcr.ArgeoNames;
import org.argeo.security.ui.admin.internal.ColumnDefinition;
import org.argeo.security.ui.admin.internal.CommonNameLP;
import org.argeo.security.ui.admin.internal.RoleIconLP;
import org.argeo.security.ui.admin.internal.UserAdminConstants;
import org.argeo.security.ui.admin.internal.UserNameLP;
import org.argeo.security.ui.admin.internal.UserTableDefaultDClickListener;
import org.argeo.security.ui.admin.internal.UserTableViewer;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
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
import org.osgi.service.useradmin.User;
import org.osgi.service.useradmin.UserAdmin;

/** Display/edit the properties common to all users */
public class UserMainPage extends FormPage implements ArgeoNames {
	final static String ID = "argeoUserEditor.mainPage";

	// private final static Log log = LogFactory.getLog(UserMainPage.class);

	private final UserEditor editor;
	private UserAdmin userAdmin;

	private char[] newPassword;

	// Local configuration
	private final int PRE_TITLE_INDENT = 10;

	public UserMainPage(FormEditor editor, UserAdmin userAdmin) {
		super(editor, ID, "Main");
		this.editor = (UserEditor) editor;
		this.userAdmin = userAdmin;
	}

	public String getNewPassword() {
		if (newPassword != null)
			return new String(newPassword);
		else
			return null;
	}

	public void resetNewPassword() {
		if (newPassword != null)
			Arrays.fill(newPassword, 'x');
		newPassword = null;
	}

	protected void createFormContent(final IManagedForm mf) {
		ScrolledForm form = mf.getForm();
		// Form page main title
		form.setText(editor.getProperty(UserAdminConstants.KEY_CN));

		// Body
		Composite body = form.getBody();
		GridLayout mainLayout = new GridLayout(1, true);
		body.setLayout(mainLayout);
		appendOverviewPart(body);
		appendPasswordPart(body);
		appendMemberOfPart(body);
	}

	/** Creates the general section */
	protected void appendOverviewPart(Composite parent) {
		FormToolkit tk = getManagedForm().getToolkit();
		Composite body = addSection(tk, parent, "Main information");
		body.setLayout(new GridLayout(2, false));

		Text distinguishedName = createLT(body, "User Name",
				editor.getProperty(UserAdminConstants.KEY_UID));
		distinguishedName.setEnabled(false);

		final Text commonName = createLT(body, "Common Name",
				editor.getProperty(UserAdminConstants.KEY_CN));
		commonName.setEnabled(false);

		final Text firstName = createLT(body, "First name",
				editor.getProperty(UserAdminConstants.KEY_FIRSTNAME));

		final Text lastName = createLT(body, "Last name",
				editor.getProperty(UserAdminConstants.KEY_LASTNAME));

		final Text email = createLT(body, "Email",
				editor.getProperty(UserAdminConstants.KEY_MAIL));

		// create form part (controller)
		AbstractFormPart part = new SectionPart((Section) body.getParent()) {
			public void commit(boolean onSave) {
				// TODO check changed ?
				// TODO Sanity checks

				editor.setProperty(UserAdminConstants.KEY_FIRSTNAME,
						firstName.getText());
				editor.setProperty(UserAdminConstants.KEY_LASTNAME,
						lastName.getText());
				editor.setProperty(UserAdminConstants.KEY_CN,
						commonName.getText());
				// TODO check mail validity
				editor.setProperty(UserAdminConstants.KEY_MAIL, email.getText());

				// Enable common name ?
				// editor.setProperty(UserAdminConstants.KEY_CN,
				// email.getText());
				super.commit(onSave);
			}
		};

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
		firstName.addModifyListener(new FormPartML(part));
		lastName.addModifyListener(new FormPartML(part));
		email.addModifyListener(new FormPartML(part));
		getManagedForm().addPart(part);
	}

	/** Creates the password section */
	protected void appendPasswordPart(Composite parent) {
		FormToolkit tk = getManagedForm().getToolkit();
		Composite body = addSection(tk, parent, "Password");

		// Section section = tk.createSection(parent, Section.TITLE_BAR);
		// section.setLayoutData(EclipseUiUtils.fillWidth());
		// section.setText("Password");
		// Composite body = tk.createComposite(section, SWT.NO_FOCUS);
		// section.setClient(body);
		// body.setLayoutData(EclipseUiUtils.fillWidth());

		body.setLayout(new GridLayout(2, false));
		// add widgets (view)
		final Text password1 = createLP(body, "New password", "");
		final Text password2 = createLP(body, "Repeat password", "");
		// create form part (controller)
		AbstractFormPart part = new SectionPart((Section) body.getParent()) {

			public void commit(boolean onSave) {
				if (!password1.getText().equals("")
						|| !password2.getText().equals("")) {
					if (password1.getText().equals(password2.getText())) {
						newPassword = password1.getText().toCharArray();
						// TODO real set password
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
		password1.addModifyListener(new FormPartML(part));
		password2.addModifyListener(new FormPartML(part));
		getManagedForm().addPart(part);
	}

	private UserTableViewer userTableViewerCmp;
	private TableViewer userViewer;

	public void appendMemberOfPart(Composite parent) {
		FormToolkit tk = getManagedForm().getToolkit();
		Composite body = addSection(tk, parent, "Groups");
		body.setLayout(EclipseUiUtils.noSpaceGridLayout());

		// Define the displayed columns
		List<ColumnDefinition> columnDefs = new ArrayList<ColumnDefinition>();
		columnDefs.add(new ColumnDefinition(new RoleIconLP(), "", 0, 24));
		columnDefs.add(new ColumnDefinition(new UserNameLP(),
				"Distinguished Name", 240));
		columnDefs.add(new ColumnDefinition(new CommonNameLP(), "Common Name",
				150));

		// Create and configure the table
		userTableViewerCmp = new MyUserTableViewer(body, SWT.MULTI
				| SWT.H_SCROLL | SWT.V_SCROLL, userAdmin);

		userTableViewerCmp.setColumnDefinitions(columnDefs);
		userTableViewerCmp.populate(true, false);
		GridData gd = EclipseUiUtils.fillAll();
		gd.heightHint = 300;
		userTableViewerCmp.setLayoutData(gd);

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
			List<User> users = new ArrayList<User>();

			// "member of" method?
			// Group group = (Group) editor.getDisplayedUser();
			// Role[] roles = group.getMembers();
			// List<User> users = new ArrayList<User>();
			// for (Role role : roles)
			// // if (role.getType() == Role.GROUP)
			// users.add((User) role);
			// return users;
			return users;
		}
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

	/** Creates label and multiline text. */
	protected Text createLMT(Composite body, String label, String value) {
		FormToolkit toolkit = getManagedForm().getToolkit();
		Label lbl = toolkit.createLabel(body, label);
		lbl.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		Text text = toolkit.createText(body, value, SWT.BORDER | SWT.MULTI);
		text.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true));
		return text;
	}

	/** Creates label and password. */
	protected Text createLP(Composite body, String label, String value) {
		FormToolkit toolkit = getManagedForm().getToolkit();
		Label lbl = toolkit.createLabel(body, label);
		lbl.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		Text text = toolkit.createText(body, value, SWT.BORDER | SWT.PASSWORD);
		text.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		return text;
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
	
	private class FormPartML implements ModifyListener {
		private static final long serialVersionUID = 6299808129505381333L;
		private AbstractFormPart formPart;

		public FormPartML(AbstractFormPart generalPart) {
			this.formPart = generalPart;
		}

		public void modifyText(ModifyEvent e) {
			formPart.markDirty();
		}
	}
}