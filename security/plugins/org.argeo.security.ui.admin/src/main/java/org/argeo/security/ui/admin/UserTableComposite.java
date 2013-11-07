package org.argeo.security.ui.admin;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import javax.jcr.query.qom.Constraint;
import javax.jcr.query.qom.Ordering;
import javax.jcr.query.qom.QueryObjectModel;
import javax.jcr.query.qom.QueryObjectModelFactory;
import javax.jcr.query.qom.Selector;
import javax.jcr.query.qom.StaticOperand;

import org.argeo.ArgeoException;
import org.argeo.eclipse.ui.EclipseUiUtils;
import org.argeo.eclipse.ui.jcr.JcrUiUtils;
import org.argeo.eclipse.ui.jcr.lists.ColumnDefinition;
import org.argeo.eclipse.ui.jcr.lists.NodeViewerComparator;
import org.argeo.eclipse.ui.jcr.lists.SimpleJcrNodeLabelProvider;
import org.argeo.eclipse.ui.specific.EclipseUiSpecificUtils;
import org.argeo.eclipse.ui.utils.ViewerUtils;
import org.argeo.jcr.ArgeoNames;
import org.argeo.jcr.ArgeoTypes;
import org.argeo.jcr.JcrUtils;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Text;

public class UserTableComposite extends Composite implements ArgeoNames {
	// private final static Log log =
	// LogFactory.getLog(UserTableComposite.class);

	private TableViewer usersViewer;
	private Text filterTxt;
	private final static String FILTER_HELP_MSG = "Type filter criterion "
			+ "separated by a space";
	private Session session;

	private Font italic;
	private Font bold;

	private boolean hasFilter;
	private boolean hasSelectionColumn;

	// private List<Node> selectedItems = new ArrayList<Node>();

	/**
	 * Overwrite to display other columns
	 */
	public List<ColumnDefinition> getColumnsDef() {
		List<ColumnDefinition> columnDefs = new ArrayList<ColumnDefinition>();

		// User ID
		columnDefs.add(new ColumnDefinition(null, ARGEO_USER_ID,
				PropertyType.STRING, "User ID", 100));
		// Displayed name
		columnDefs.add(new ColumnDefinition(null, Property.JCR_TITLE,
				PropertyType.STRING, "Name", 150));

		// E-mail
		columnDefs.add(new ColumnDefinition(null, ARGEO_PRIMARY_EMAIL,
				PropertyType.STRING, "E-mail", 150));

		// Description
		columnDefs.add(new ColumnDefinition(null, Property.JCR_DESCRIPTION,
				PropertyType.STRING, "Description", 200));

		return columnDefs;
	}

	public UserTableComposite(Composite parent, int style, Session session) {
		super(parent, style);
		this.session = session;
	}

	/**
	 * 
	 * @param addFilter
	 *            choose to add a field to filter results or not
	 * @param addSelection
	 *            choose to add a column to select some of the displayed results
	 *            or not
	 */
	public void populate(boolean addFilter, boolean addSelection) {
		// initialization
		Composite parent = this;
		italic = EclipseUiUtils.getItalicFont(parent);
		bold = EclipseUiUtils.getBoldFont(parent);
		hasFilter = addFilter;
		hasSelectionColumn = addSelection;

		// Main Layout
		this.setLayout(new GridLayout(1, false));
		if (hasFilter)
			createFilterPart(parent);
		usersViewer = createTableViewer(parent);
		EclipseUiSpecificUtils.enableToolTipSupport(usersViewer);
		usersViewer.setContentProvider(new UsersContentProvider());
		refreshFilteredList();
	}

	public List<Node> getSelectedUsers() {
		if (hasSelectionColumn) {
			Object[] elements = ((CheckboxTableViewer) usersViewer)
					.getCheckedElements();

			List<Node> result = new ArrayList<Node>();
			for (Object obj : elements) {
				result.add((Node) obj);
			}
			return result;
		} else
			throw new ArgeoException("Unvalid request: no selection column "
					+ "has been created for the current table");
	}

	/** Returns the User table viewer, typically to add doubleclick listener */
	public TableViewer getTableViewer() {
		return usersViewer;
	}

	private TableViewer createTableViewer(final Composite parent) {
		int style = SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL;
		if (hasSelectionColumn)
			style = style | SWT.CHECK;

		Table table = new Table(parent, style);
		table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		TableViewer viewer;
		if (hasSelectionColumn)
			viewer = new CheckboxTableViewer(table);
		else
			viewer = new TableViewer(table);
		table.setLinesVisible(true);
		table.setHeaderVisible(true);

		// pass a mapping between col index and property name to the comparator.
		// List<String> propertiesList = new ArrayList<String>();

		TableViewerColumn column;
		int offset = 0;
		// if (hasSelectionColumn) {
		if (hasSelectionColumn) {
			offset = 1;
			column = ViewerUtils.createTableViewerColumn(viewer, "", SWT.NONE,
					25);
			// column.setEditingSupport(new SelectedEditingSupport(viewer));
			column.setLabelProvider(new ColumnLabelProvider() {
				@Override
				public String getText(Object element) {
					return null;
				}
			});
			SelectionAdapter selectionAdapter = new SelectionAdapter() {
				boolean allSelected = false;

				@Override
				public void widgetSelected(SelectionEvent e) {
					allSelected = !allSelected;
					((CheckboxTableViewer) usersViewer)
							.setAllChecked(allSelected);
				}
			};
			column.getColumn().addSelectionListener(selectionAdapter);
		}

		// Create other columns
		List<ColumnDefinition> colDefs = getColumnsDef();

		NodeViewerComparator comparator = new NodeViewerComparator();
		int i = offset;
		for (ColumnDefinition colDef : colDefs) {
			column = ViewerUtils.createTableViewerColumn(viewer,
					colDef.getHeaderLabel(), SWT.NONE, colDef.getColumnSize());
			column.setLabelProvider(new CLProvider(colDef.getPropertyName()));
			column.getColumn().addSelectionListener(
					JcrUiUtils.getNodeSelectionAdapter(i,
							colDef.getPropertyType(), colDef.getPropertyName(),
							comparator, viewer));
			i++;
		}

		// IMPORTANT: initialize comparator before setting it
		ColumnDefinition firstCol = colDefs.get(0);
		comparator.setColumn(firstCol.getPropertyType(),
				firstCol.getPropertyName());
		viewer.setComparator(comparator);

		return viewer;
	}

	// private class SelectedEditingSupport extends EditingSupport {
	// private final TableViewer viewer;
	//
	// public SelectedEditingSupport(TableViewer viewer) {
	// super(viewer);
	// this.viewer = viewer;
	// }
	//
	// @Override
	// protected CellEditor getCellEditor(Object element) {
	// return new CheckboxCellEditor(viewer.getTable());
	// }
	//
	// @Override
	// protected boolean canEdit(Object element) {
	// return true;
	// }
	//
	// @Override
	// protected Object getValue(Object element) {
	// return selectedItems.contains(element);
	// }
	//
	// @Override
	// protected void setValue(Object element, Object value) {
	// if ((Boolean) value && !selectedItems.contains(element))
	// selectedItems.add((Node) element);
	// else if (!(Boolean) value && selectedItems.contains(element))
	// selectedItems.remove((Node) element);
	// viewer.update(element, null);
	// }
	// }

	private class CLProvider extends SimpleJcrNodeLabelProvider {

		public CLProvider(String propertyName) {
			super(propertyName);
		}

		public String getToolTipText(Object element) {
			return getText(element);
		}

		@Override
		public Font getFont(Object elem) {
			// self
			String username = getProperty(elem, ARGEO_USER_ID);
			if (username.equals(session.getUserID()))
				return bold;

			// disabled
			try {
				Node userProfile = (Node) elem;
				// Node userProfile = userHome.getNode(ARGEO_PROFILE);
				if (!userProfile.getProperty(ARGEO_ENABLED).getBoolean())
					return italic;
				else
					return null;
			} catch (RepositoryException e) {
				throw new ArgeoException("Cannot get font for " + username, e);
			}
		}
	}

	@Override
	public boolean setFocus() {
		usersViewer.getTable().setFocus();
		return true;
	}

	@Override
	public void dispose() {
		super.dispose();
	}

	public void refresh() {
		refreshFilteredList();
	}

	private String getProperty(Object element, String name) {
		try {
			Node userProfile = (Node) element;
			return userProfile.hasProperty(name) ? userProfile
					.getProperty(name).getString() : "";
		} catch (RepositoryException e) {
			throw new ArgeoException("Cannot get property " + name, e);
		}
	}

	private class UsersContentProvider implements IStructuredContentProvider {

		public Object[] getElements(Object inputElement) {
			return (Object[]) inputElement;
		}

		public void dispose() {
		}

		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		}
	}

	/* MANAGE FILTER */
	private void createFilterPart(Composite parent) {
		// Text Area for the filter
		filterTxt = new Text(parent, SWT.BORDER | SWT.SEARCH | SWT.ICON_SEARCH
				| SWT.ICON_CANCEL);
		filterTxt.setMessage(FILTER_HELP_MSG);
		filterTxt.setLayoutData(new GridData(GridData.GRAB_HORIZONTAL
				| GridData.HORIZONTAL_ALIGN_FILL));
		filterTxt.addModifyListener(new ModifyListener() {

			public void modifyText(ModifyEvent event) {
				refreshFilteredList();
			}
		});
	}

	/**
	 * Refresh the user list: caller might overwrite in order to display a
	 * subset of all users
	 */
	protected void refreshFilteredList() {
		// if (hasSelectionColumn)
		// selectedItems.clear();

		List<Node> nodes;
		try {
			nodes = JcrUtils.nodeIteratorToList(listFilteredElements(session,
					hasFilter ? filterTxt.getText() : null));
			usersViewer.setInput(nodes.toArray());
		} catch (RepositoryException e) {
			throw new ArgeoException("Unable to list users", e);
		}
	}

	// /**
	// * Enable a children class to directly set an array of users to the
	// viewer,
	// * when such a behaviour cannot be achieved by overwriting the
	// * refreshFilteredList() method
	// */
	// protected void setFilteredList(Object[] nodes) {
	// // if (hasSelectionColumn)
	// // selectedItems.clear();
	// usersViewer.setInput(nodes);
	// }

	/**
	 * Build repository request : caller might overwrite in order to display a
	 * subset of all users
	 */
	protected NodeIterator listFilteredElements(Session session, String filter)
			throws RepositoryException {
		QueryManager queryManager = session.getWorkspace().getQueryManager();
		QueryObjectModelFactory factory = queryManager.getQOMFactory();

		Selector source = factory.selector(ArgeoTypes.ARGEO_USER_PROFILE,
				ArgeoTypes.ARGEO_USER_PROFILE);

		// // Create a dynamic operand for each property on which we want to
		// filter
		// DynamicOperand userIdDO = factory.propertyValue(
		// source.getSelectorName(), ARGEO_USER_ID);
		// DynamicOperand fullNameDO = factory.propertyValue(
		// source.getSelectorName(), Property.JCR_TITLE);
		// DynamicOperand mailDO =
		// factory.propertyValue(source.getSelectorName(),
		// ARGEO_PRIMARY_EMAIL);

		// Default Constraint: no source artifacts
		Constraint defaultC = null;
		// Build constraints based the textArea content
		if (filter != null && !"".equals(filter.trim())) {
			// Parse the String
			String[] strs = filter.trim().split(" ");
			for (String token : strs) {
				StaticOperand so = factory.literal(session.getValueFactory()
						.createValue("*" + token + "*"));
				Constraint currC = factory.fullTextSearch(
						source.getSelectorName(), null, so);
				if (defaultC == null)
					defaultC = currC;
				else
					defaultC = factory.and(defaultC, currC);
			}
		}

		Ordering order = factory.ascending(factory.propertyValue(
				source.getSelectorName(), ARGEO_USER_ID));
		Ordering[] orderings = { order };

		QueryObjectModel query = factory.createQuery(source, defaultC,
				orderings, null);

		QueryResult result = query.execute();
		return result.getNodes();
	}
}