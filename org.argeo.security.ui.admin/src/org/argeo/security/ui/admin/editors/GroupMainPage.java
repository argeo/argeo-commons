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
import org.argeo.eclipse.ui.utils.ViewerUtils;
import org.argeo.jcr.ArgeoNames;
import org.argeo.security.ui.admin.SecurityAdminImages;
import org.argeo.security.ui.admin.UserAdminConstants;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.AbstractFormPart;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.SectionPart;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;
import org.osgi.service.useradmin.Role;
import org.osgi.service.useradmin.User;
import org.osgi.service.useradmin.UserAdmin;

/** Display/edit the properties of the groups */
public class GroupMainPage extends FormPage implements ArgeoNames {
	final static String ID = "GroupEditor.mainPage";

	private final UserEditor editor;

	private UserAdmin userAdmin;

	public GroupMainPage(FormEditor editor, UserAdmin userAdmin) {
		super(editor, ID, "Main");
		this.editor = (UserEditor) editor;
		this.userAdmin = userAdmin;
	}

	protected void createFormContent(final IManagedForm mf) {
		try {
			ScrolledForm form = mf.getForm();
			refreshFormTitle(form);
			GridLayout mainLayout = new GridLayout(1, true);
			form.getBody().setLayout(mainLayout);

			createGeneralPart(form.getBody());
		} catch (RepositoryException e) {
			throw new ArgeoException("Cannot create form content", e);
		}
	}

	/** Creates the general section */
	protected void createGeneralPart(Composite parent)
			throws RepositoryException {
		FormToolkit tk = getManagedForm().getToolkit();
		Section section = tk.createSection(parent, Section.TITLE_BAR);
		section.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		section.setText("Members of "
				+ editor.getProperty(UserAdminConstants.KEY_CN));
		Composite body = tk.createComposite(section, SWT.WRAP);
		section.setClient(body);
		body.setLayoutData(EclipseUiUtils.fillAll());

		body.setLayout(new GridLayout());
		new Label(body, SWT.NONE)
				.setText("Display a table with members for this group");

		// final Text firstName = createLT(body, "First name",
		// getProperty(ARGEO_FIRST_NAME));
		// final Text lastName = createLT(body, "Last name",
		// getProperty(ARGEO_LAST_NAME));
		// final Text email = createLT(body, "Email",
		// editor.getProperty(UserAdminConstants.KEY_MAIL));
		// // final Text description = createLMT(body, "Description",
		// getProperty(Property.JCR_DESCRIPTION));

		// create form part (controller)
		AbstractFormPart part = new SectionPart(section) {
			public void commit(boolean onSave) {
				// TODO check mail validity
				// editor.setProperty(UserAdminConstants.KEY_MAIL,
				// email.getText());

				// userProfile.getSession().getWorkspace().getVersionManager()
				// .checkout(userProfile.getPath());
				// userProfile.setProperty(Property.JCR_TITLE,
				// commonName.getText());
				// userProfile.setProperty(ARGEO_FIRST_NAME,
				// firstName.getText());
				// userProfile
				// .setProperty(ARGEO_LAST_NAME, lastName.getText());
				// userProfile.setProperty(ARGEO_PRIMARY_EMAIL,
				// email.getText());
				// userProfile.setProperty(Property.JCR_DESCRIPTION,
				// description.getText());
				// userProfile.getSession().save();
				// userProfile.getSession().getWorkspace().getVersionManager()
				// .checkin(userProfile.getPath());
				super.commit(onSave);
			}
		};
		// if (username != null)
		// username.addModifyListener(new FormPartML(part));
		// commonName.addModifyListener(new FormPartML(part));
		// firstName.addModifyListener(new FormPartML(part));
		// lastName.addModifyListener(new FormPartML(part));

		// email.addModifyListener(new FormPartML(part));
		getManagedForm().addPart(part);
	}

	private void refreshFormTitle(ScrolledForm form) throws RepositoryException {
		// form.setText(getProperty(Property.JCR_TITLE)
		// + (userProfile.getProperty(ARGEO_ENABLED).getBoolean() ? ""
		// : " [DISABLED]"));
	}

	// /** @return the property, or the empty string if not set */
	// protected String getProperty(String name) throws RepositoryException {
	// return userProfile.hasProperty(name) ? userProfile.getProperty(name)
	// .getString() : "";
	// }

	/** Creates label and text. */
	protected Text createLT(Composite body, String label, String value) {
		FormToolkit toolkit = getManagedForm().getToolkit();
		Label lbl = toolkit.createLabel(body, label);
		lbl.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		Text text = toolkit.createText(body, value, SWT.BORDER);
		text.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		return text;
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

	// Manage the user table
	public List<ColumnDefinition> getColumnsDef() {
		List<ColumnDefinition> columnDefs = new ArrayList<ColumnDefinition>();

		// Icon
		columnDefs.add(new ColumnDefinition(new UserNameLP(), "", 0, 26));
		// Distinguished Name
		columnDefs.add(new ColumnDefinition(new CommonNameLP(),
				"Distinguished Name", 150));
		// Displayed name
		columnDefs.add(new ColumnDefinition(new CommonNameLP(), "Common Name",
				150));
		return columnDefs;
	}

	private void createUserTable(Composite parent) {

		// Main Layout
		GridLayout layout = EclipseUiUtils.noSpaceGridLayout();
		layout.verticalSpacing = 5;
		parent.setLayout(layout);

		// usersViewer = createTableViewer(parent);
		// usersViewer.setContentProvider(new UsersContentProvider());

		// Really?
		refreshFilteredList(null);

		// Configure
		// usersViewer.addDoubleClickListener(new ViewDoubleClickListener());
		// getViewSite().setSelectionProvider(usersViewer);
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

	/**
	 * Refresh the user list: caller might overwrite in order to display a
	 * subset of all users, typically to remove current user from the list
	 */
	protected void refreshFilteredList(String filter) {
		// try {
		// // Role[] roles = userAdmin.getRoles(filter);
		// // List<User> users = new ArrayList<User>();
		// // for (Role role : roles)
		// // if (role.getType() == Role.USER && role.getType() != Role.GROUP)
		// // users.add((User) role);
		// // usersViewer.setInput(users.toArray());
		// } catch (InvalidSyntaxException e) {
		// throw new ArgeoException("Unable to get roles with filter: "
		// + filter, e);
		// }
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

	private class IconLP extends UseradminAbstractLP {
		private static final long serialVersionUID = 6550449442061090388L;

		@Override
		public String getText(User user) {
			return "";
		}

		@Override
		public Image getImage(Object element) {
			User user = (User) element;
			if (user.getType() == Role.GROUP)
				return SecurityAdminImages.ICON_GROUP;
			else
				return SecurityAdminImages.ICON_USER;
		}
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
			Object obj = user.getProperties().get(UserAdminConstants.KEY_CN);
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

		public ColumnDefinition(ColumnLabelProvider provider, String label,
				int weight) {
			this.provider = provider;
			this.label = label;
			this.weight = weight;
			this.minWidth = weight;
		}

		public ColumnDefinition(ColumnLabelProvider provider, String label,
				int weight, int minWidth) {
			this.provider = provider;
			this.label = label;
			this.weight = weight;
			this.minWidth = minWidth;

		}
	}
}