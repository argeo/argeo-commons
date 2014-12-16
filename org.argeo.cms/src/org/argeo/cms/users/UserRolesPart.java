package org.argeo.cms.users;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Item;
import javax.jcr.RepositoryException;

import org.argeo.ArgeoException;
import org.argeo.cms.CmsUtils;
import org.argeo.cms.viewers.EditablePart;
import org.argeo.cms.viewers.NodePart;
import org.argeo.cms.widgets.StyledControl;
import org.argeo.jcr.ArgeoNames;
import org.argeo.security.UserAdminService;
import org.argeo.security.jcr.JcrSecurityModel;
import org.argeo.security.jcr.JcrUserDetails;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Text;
import org.springframework.security.GrantedAuthority;

/** Display a single user main info once it has been created. */
public class UserRolesPart extends StyledControl implements EditablePart,
		NodePart, FocusListener {

	// A static list of supported properties.
	private List<Text> texts;
	private final static String KEY_PROP_NAME = "jcr:propertyName";

	private CheckboxTableViewer rolesViewer;
	private JcrUserDetails userDetails;
	private UserAdminService userAdminService;
	private List<String> roles;

	// FIXME
	// private final Image checked;

	// TODO implement to provide user creation ability for anonymous user?
	// public UserPart(Composite parent, int swtStyle) {
	// super(parent, swtStyle);
	// }

	public UserRolesPart(Composite parent, int style, Item item,
			UserAdminService userAdminService, JcrSecurityModel jcrSecurityModel)
			throws RepositoryException {
		this(parent, style, item, true);
	}

	public UserRolesPart(Composite parent, int style, Item item,
			boolean cacheImmediately) throws RepositoryException {
		super(parent, style, item, cacheImmediately);

		// checked = new Image(parent, imageData);

	}

	@Override
	public Item getItem() throws RepositoryException {
		return getNode();
	}

	// Experimental, remove
	public void setMouseListener(MouseListener mouseListener) {
		super.setMouseListener(mouseListener);

		for (Text txt : texts)
			txt.addMouseListener(mouseListener);

	}

	@Override
	protected Control createControl(Composite box, String style) {
		Table table = new Table(box, SWT.CHECK | SWT.MULTI | SWT.H_SCROLL
				| SWT.V_SCROLL);
		table.setLinesVisible(true);
		table.setHeaderVisible(false);
		CmsUtils.style(table, style);

		rolesViewer = new CheckboxTableViewer(table);

		TableViewerColumn column;

		// check column
		// TableViewerColumn column = createTableViewerColumn(rolesViewer,
		// "checked", 20);
		// column.setLabelProvider(new ColumnLabelProvider() {
		// public String getText(Object element) {
		// return null;
		// }

		// public Image getImage(Object element) {
		// String role = element.toString();
		// if (roles.contains(role)) {
		//
		// return ROLE_CHECKED;
		// } else {
		// return null;
		// }
		//
		// }
		// );
		// column.setEditingSupport(new RoleEditingSupport(rolesViewer, part));

		// role column
		column = createTableViewerColumn(rolesViewer, "Role", 400);
		column.setLabelProvider(new ColumnLabelProvider() {
			private static final long serialVersionUID = -7334412925967366255L;

			public String getText(Object element) {
				return element.toString();
			}
		});
		rolesViewer.setContentProvider(new RolesContentProvider());
		rolesViewer.setInput(userAdminService.listEditableRoles().toArray());

		rolesViewer.addCheckStateListener(new ICheckStateListener() {

			@Override
			public void checkStateChanged(CheckStateChangedEvent event) {
				String name = (String) event.getElement();
				boolean contained = roles.contains(name);
				boolean checked = event.getChecked();
				if (checked != contained) {
					if (contained)
						roles.add(name);
					else
						roles.remove(name);
					userDetails = userDetails.cloneWithNewRoles(roles);
					userAdminService.updateUser(userDetails);
				}
			}
		});
		return table;
	}

	// void refresh() {
	// for (Text txt : texts) {
	// }
	// }

	// THE LISTENER
	@Override
	public void focusGained(FocusEvent e) {
		// Do nothing
	}

	@Override
	public void focusLost(FocusEvent e) {
		// Save change if needed
		// Text text = (Text) e.getSource();
	}

	// private final static Image ROLE_CHECKED = SecurityAdminPlugin
	// .getImageDescriptor("icons/security.gif").createImage();

	public void setUserAdminService(UserAdminService userAdminService) {
		this.userAdminService = userAdminService;

		try {
			String username = getNode().getProperty(ArgeoNames.ARGEO_USER_ID)
					.getString();
			// ;

			if (userAdminService.userExists(username)) {
				JcrUserDetails userDetails = (JcrUserDetails) userAdminService
						.loadUserByUsername(username);
				setUserDetails(userDetails);
			}
		} catch (Exception e) {
			throw new ArgeoException("Cannot retrieve userDetails for "// +
																		// username
					, e);
		}

	}

	public void setUserDetails(JcrUserDetails userDetails) {
		this.userDetails = userDetails;

		this.roles = new ArrayList<String>();
		for (GrantedAuthority ga : userDetails.getAuthorities())
			roles.add(ga.getAuthority());
		if (rolesViewer != null)
			rolesViewer.refresh();
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

	public List<String> getRoles() {
		return roles;
	}

	public void refresh() {

		// return roles.toArray();
		rolesViewer.setCheckedElements(roles.toArray()); // setSelection(1);
		// rolesViewer.setInput(roles);
		rolesViewer.refresh();
	}

	private class RolesContentProvider implements IStructuredContentProvider {
		private static final long serialVersionUID = 4119915828862214310L;

		public Object[] getElements(Object inputElement) {
			return userAdminService.listEditableRoles().toArray();
		}

		public void dispose() {
		}

		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			viewer.refresh();
		}
	}

	// /** Select the columns by editing the checkbox in the first column */
	// class RoleEditingSupport extends EditingSupport {
	//
	// private final TableViewer viewer;
	//
	// public RoleEditingSupport(TableViewer viewer) {
	// super(viewer);
	// this.viewer = viewer;
	// }
	//
	// @Override
	// protected CellEditor getCellEditor(Object element) {
	// return new CheckboxCellEditor(null, SWT.CHECK | SWT.READ_ONLY);
	//
	// }
	//
	// @Override
	// protected boolean canEdit(Object element) {
	// return true;
	// }
	//
	// @Override
	// protected Object getValue(Object element) {
	// String role = element.toString();
	// return roles.contains(role);
	//
	// }
	//
	// @Override
	// protected void setValue(Object element, Object value) {
	// Boolean inRole = (Boolean) value;
	// String role = element.toString();
	// if (inRole && !roles.contains(role)) {
	// roles.add(role);
	// } else if (!inRole && roles.contains(role)) {
	// roles.remove(role);
	// }
	// viewer.refresh();
	// }
	// }

}