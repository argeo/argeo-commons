package org.argeo.security.ui.editors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.argeo.security.ArgeoSecurityService;
import org.argeo.security.ArgeoUser;
import org.argeo.security.nature.SimpleUserNature;
import org.argeo.security.ui.SecurityUiPlugin;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.CheckboxCellEditor;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.IStructuredContentProvider;
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
import org.eclipse.ui.forms.AbstractFormPart;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.SectionPart;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.ColumnLayout;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;

/**
 * Display/edit the properties common to all {@link ArgeoUser} (username and
 * roles) as well as the properties of the {@link SimpleUserNature}.
 */
public class DefaultUserMainPage extends FormPage {
	final static String ID = "argeoUserEditor.mainPage";

	private final static Image ROLE_CHECKED = SecurityUiPlugin
			.getImageDescriptor("icons/security.gif").createImage();
	private final static Log log = LogFactory.getLog(ArgeoUserEditor.class);

	private ArgeoUser user;
	private SimpleUserNature simpleNature;
	private String simpleNatureType;
	private ArgeoSecurityService securityService;

	public DefaultUserMainPage(FormEditor editor,
			ArgeoSecurityService securityService, ArgeoUser user) {
		super(editor, ID, "Main");
		this.securityService = securityService;
		this.user = user;
		this.simpleNature = SimpleUserNature.findSimpleUserNature(user,
				simpleNatureType);
	}

	protected void createFormContent(final IManagedForm mf) {
		ScrolledForm form = mf.getForm();
		form.setText(simpleNature.getFirstName() + " "
				+ simpleNature.getLastName());
		ColumnLayout mainLayout = new ColumnLayout();
		mainLayout.minNumColumns = 1;
		mainLayout.maxNumColumns = 4;
		mainLayout.topMargin = 0;
		mainLayout.bottomMargin = 5;
		mainLayout.leftMargin = mainLayout.rightMargin = mainLayout.horizontalSpacing = mainLayout.verticalSpacing = 10;
		form.getBody().setLayout(mainLayout);

		createGeneralPart(form.getBody());
		createRolesPart(form.getBody());
	}

	/** Creates the general section */
	protected void createGeneralPart(Composite parent) {
		FormToolkit tk = getManagedForm().getToolkit();
		Section section = tk.createSection(parent, Section.TITLE_BAR);
		section.setText("General");

		Composite body = tk.createComposite(section, SWT.WRAP);
		section.setClient(body);
		GridLayout layout = new GridLayout();
		layout.marginWidth = layout.marginHeight = 0;
		layout.numColumns = 2;
		body.setLayout(layout);

		// add widgets (view)
		tk.createLabel(body, "Username");
		tk.createLabel(body, user.getUsername());
		final Text firstName = createLT(body, "First name",
				simpleNature.getFirstName());
		final Text lastName = createLT(body, "Last name",
				simpleNature.getLastName());
		final Text email = createLT(body, "Email", simpleNature.getEmail());
		final Text description = createLT(body, "Description",
				simpleNature.getDescription());

		// create form part (controller)
		AbstractFormPart part = new SectionPart(section) {
			public void commit(boolean onSave) {
				simpleNature.setFirstName(firstName.getText());
				simpleNature.setLastName(lastName.getText());
				simpleNature.setEmail(email.getText());
				simpleNature.setDescription(description.getText());
				super.commit(onSave);
				if (log.isDebugEnabled())
					log.debug("General part committed");
			}
		};
		firstName.addModifyListener(new FormPartML(part));
		lastName.addModifyListener(new FormPartML(part));
		email.addModifyListener(new FormPartML(part));
		description.addModifyListener(new FormPartML(part));
		getManagedForm().addPart(part);
	}

	/** Creates the role section */
	protected void createRolesPart(Composite parent) {
		FormToolkit tk = getManagedForm().getToolkit();
		Section section = tk.createSection(parent, Section.DESCRIPTION
				| Section.TITLE_BAR);
		section.setText("Roles");
		section.setDescription("Roles define "
				+ "the authorizations for this user.");
		Table table = new Table(section, SWT.MULTI | SWT.H_SCROLL
				| SWT.V_SCROLL);
		section.setClient(table);

		AbstractFormPart part = new SectionPart(section) {
			public void commit(boolean onSave) {
				// roles have already been modified in editing
				super.commit(onSave);
				if (log.isDebugEnabled())
					log.debug("Role part committed");
			}
		};
		getManagedForm().addPart(part);

		GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
		gridData.verticalSpan = 20;
		table.setLayoutData(gridData);
		table.setLinesVisible(true);
		table.setHeaderVisible(false);
		TableViewer viewer = new TableViewer(table);

		// check column
		TableViewerColumn column = createTableViewerColumn(viewer, "checked",
				20);
		column.setLabelProvider(new ColumnLabelProvider() {
			public String getText(Object element) {
				return null;
			}

			public Image getImage(Object element) {
				String role = element.toString();
				if (user.getRoles().contains(role)) {
					return ROLE_CHECKED;
				} else {
					return null;
				}
			}
		});
		column.setEditingSupport(new RoleEditingSupport(viewer, part));

		// role column
		column = createTableViewerColumn(viewer, "Role", 200);
		column.setLabelProvider(new ColumnLabelProvider() {
			public String getText(Object element) {
				return element.toString();
			}

			public Image getImage(Object element) {
				return null;
			}
		});
		viewer.setContentProvider(new RolesContentProvider());
		viewer.setInput(getEditorSite());
	}

	protected TableViewerColumn createTableViewerColumn(TableViewer viewer,
			String title, int bound) {
		final TableViewerColumn viewerColumn = new TableViewerColumn(viewer,
				SWT.NONE);
		final TableColumn column = viewerColumn.getColumn();
		column.setText(title);
		column.setWidth(bound);
		column.setResizable(true);
		column.setMoveable(true);
		return viewerColumn;

	}

	/** Creates label and text. */
	protected Text createLT(Composite body, String label, String value) {
		FormToolkit toolkit = getManagedForm().getToolkit();
		toolkit.createLabel(body, label);
		Text text = toolkit.createText(body, value, SWT.BORDER);
		text.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		return text;
	}

	public void setSimpleNatureType(String simpleNatureType) {
		this.simpleNatureType = simpleNatureType;
	}

	private class FormPartML implements ModifyListener {
		private AbstractFormPart formPart;

		public FormPartML(AbstractFormPart generalPart) {
			this.formPart = generalPart;
		}

		public void modifyText(ModifyEvent e) {
			formPart.markDirty();
		}

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

	/** Select the columns by editing the checkbox in the first column */
	class RoleEditingSupport extends EditingSupport {

		private final TableViewer viewer;
		private final AbstractFormPart formPart;

		public RoleEditingSupport(TableViewer viewer, AbstractFormPart formPart) {
			super(viewer);
			this.viewer = viewer;
			this.formPart = formPart;
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
			if (inRole && !user.getRoles().contains(role)) {
				user.getRoles().add(role);
				formPart.markDirty();
			} else if (!inRole && user.getRoles().contains(role)) {
				user.getRoles().remove(role);
				formPart.markDirty();
			}
			viewer.refresh();
		}
	}

}
