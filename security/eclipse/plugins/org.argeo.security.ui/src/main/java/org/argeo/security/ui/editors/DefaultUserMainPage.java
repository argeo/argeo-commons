package org.argeo.security.ui.editors;

import org.argeo.security.ArgeoSecurityService;
import org.argeo.security.ArgeoUser;
import org.argeo.security.nature.SimpleUserNature;
import org.argeo.security.ui.SecurityUiPlugin;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.CheckboxCellEditor;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.ColumnLayout;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;

public class DefaultUserMainPage extends FormPage {
	// We use icons
	private static final Image CHECKED = SecurityUiPlugin.getImageDescriptor(
			"icons/security.gif").createImage();

	private ArgeoUser user;
	private SimpleUserNature simpleNature;

	private String simpleNatureType;

	private Text firstName;
	private Text lastName;
	private Text email;
	private Text description;
	private TableViewer rolesViewer;

	private ArgeoSecurityService securityService;

	public DefaultUserMainPage(FormEditor editor,
			ArgeoSecurityService securityService, ArgeoUser user) {
		super(editor, "argeoUserEditor.mainPage", "Main");
		this.securityService = securityService;
		this.user = user;
		this.simpleNature = SecurityUiPlugin.findSimpleUserNature(user,
				simpleNatureType);
	}

	protected void createFormContent(final IManagedForm mf) {
		ScrolledForm form = mf.getForm();

		// Set the title of the current form
		form.setText(simpleNature.getFirstName() + " "
				+ simpleNature.getLastName());

		ColumnLayout mainLayout = new ColumnLayout();
		mainLayout.minNumColumns = 1;
		mainLayout.maxNumColumns = 4;

		mainLayout.topMargin = 0;
		mainLayout.bottomMargin = 5;
		mainLayout.leftMargin = mainLayout.rightMargin = mainLayout.horizontalSpacing = mainLayout.verticalSpacing = 10;
		form.getBody().setLayout(mainLayout);

		FormToolkit tk = mf.getToolkit();

		Composite body = tk.createComposite(form.getBody());
		GridLayout layout = new GridLayout();
		layout.marginWidth = layout.marginHeight = 0;
		layout.numColumns = 2;
		body.setLayout(layout);

		// Comments
		tk.createLabel(body, "Username");
		tk.createLabel(body, user.getUsername());

		firstName = createLT(mf, body, "First name",
				simpleNature.getFirstName());
		lastName = createLT(mf, body, "Last name", simpleNature.getLastName());
		email = createLT(mf, body, "Email", simpleNature.getEmail());
		description = createLT(mf, body, "Description",
				simpleNature.getDescription());

		// Roles table
		tk.createLabel(body, "Roles");
		Table table = new Table(body, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
		GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
		gridData.verticalSpan = 20;
		table.setLayoutData(gridData);
		table.setLinesVisible(true);
		table.setHeaderVisible(false);
		rolesViewer = new TableViewer(table);

		// check column
		TableViewerColumn column = createTableViewerColumn("checked", 20);
		column.setLabelProvider(new ColumnLabelProvider() {
			public String getText(Object element) {
				return null;
			}

			public Image getImage(Object element) {
				String role = element.toString();
				if (user.getRoles().contains(role)) {
					return CHECKED;
				} else {
					return null;
				}
			}
		});
		column.setEditingSupport(new RoleEditingSupport(rolesViewer));

		// role column
		column = createTableViewerColumn("Role", 200);
		column.setLabelProvider(new ColumnLabelProvider() {
			public String getText(Object element) {
				return element.toString();
			}

			public Image getImage(Object element) {
				return null;
			}
		});
		rolesViewer.setContentProvider(new RolesContentProvider());
		rolesViewer.setInput(getEditorSite());

	}

	protected TableViewerColumn createTableViewerColumn(String title, int bound) {
		final TableViewerColumn viewerColumn = new TableViewerColumn(
				rolesViewer, SWT.NONE);
		final TableColumn column = viewerColumn.getColumn();
		column.setText(title);
		column.setWidth(bound);
		column.setResizable(true);
		column.setMoveable(true);
		return viewerColumn;

	}

	/** Creates label and text. */
	protected Text createLT(final IManagedForm managedForm, Composite body,
			String label, String value) {
		FormToolkit toolkit = managedForm.getToolkit();
		toolkit.createLabel(body, label);
		Text text = toolkit.createText(body, value, SWT.BORDER);
		text.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		text.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				managedForm.dirtyStateChanged();
			}
		});
		return text;
	}

	public void setSimpleNatureType(String simpleNatureType) {
		this.simpleNatureType = simpleNatureType;
	}

	private class RolesContentProvider implements IStructuredContentProvider {
		public Object[] getElements(Object inputElement) {
			return securityService.getSecurityDao().listEditableRoles()
					.toArray();
		}

		public void dispose() {
		}

		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		}
	}

	private class RolesLabelProvider extends LabelProvider implements
			ITableLabelProvider {
		public String getColumnText(Object element, int columnIndex) {
			// Principal argeoUser = (Principal) element;
			// switch (columnIndex) {
			// case 0:
			// return argeoUser.getName();
			// case 1:
			// return argeoUser.toString();
			// default:
			// throw new ArgeoException("Unmanaged column " + columnIndex);
			// }
			return element.toString();
		}

		public Image getColumnImage(Object element, int columnIndex) {
			// TODO Auto-generated method stub
			return null;
		}

	}

	class RoleEditingSupport extends EditingSupport {

		private final TableViewer viewer;

		public RoleEditingSupport(TableViewer viewer) {
			super(viewer);
			this.viewer = viewer;
		}

		@Override
		protected CellEditor getCellEditor(Object element) {
			return new CheckboxCellEditor(null, SWT.CHECK | SWT.READ_ONLY);

		}

		@Override
		protected boolean canEdit(Object element) {
			return true;
		}

		@Override
		protected Object getValue(Object element) {
			String role = element.toString();
			return user.getRoles().contains(role);

		}

		@Override
		protected void setValue(Object element, Object value) {
			Boolean inRole = (Boolean) value;
			String role = element.toString();
			if (inRole && !user.getRoles().contains(role))
				user.getRoles().add(role);
			else if (!inRole && user.getRoles().contains(role))
				user.getRoles().remove(role);
			viewer.refresh();
		}
	}

}
