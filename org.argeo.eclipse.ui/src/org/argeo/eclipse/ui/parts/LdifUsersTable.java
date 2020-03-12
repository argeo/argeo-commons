package org.argeo.eclipse.ui.parts;

import java.util.ArrayList;
import java.util.List;

import org.argeo.eclipse.ui.ColumnDefinition;
import org.argeo.eclipse.ui.EclipseUiException;
import org.argeo.eclipse.ui.EclipseUiUtils;
import org.argeo.eclipse.ui.util.ViewerUtils;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Text;
import org.osgi.service.useradmin.User;

/**
 * Generic composite that display a filter and a table viewer to display users
 * (can also be groups)
 * 
 * Warning: this class does not extends <code>TableViewer</code>. Use the
 * getTableViewer method to access it.
 * 
 */
public abstract class LdifUsersTable extends Composite {
	private static final long serialVersionUID = -7385959046279360420L;

	// Context
	// private UserAdmin userAdmin;

	// Configuration
	private List<ColumnDefinition> columnDefs = new ArrayList<ColumnDefinition>();
	private boolean hasFilter;
	private boolean preventTableLayout = false;
	private boolean hasSelectionColumn;
	private int tableStyle;

	// Local UI Objects
	private TableViewer usersViewer;
	private Text filterTxt;

	/* EXPOSED METHODS */

	/**
	 * @param parent
	 * @param style
	 */
	public LdifUsersTable(Composite parent, int style) {
		super(parent, SWT.NO_FOCUS);
		this.tableStyle = style;
	}

	// TODO workaround the bug of the table layout in the Form
	public LdifUsersTable(Composite parent, int style, boolean preventTableLayout) {
		super(parent, SWT.NO_FOCUS);
		this.tableStyle = style;
		this.preventTableLayout = preventTableLayout;
	}

	/** This must be called before the call to populate method */
	public void setColumnDefinitions(List<ColumnDefinition> columnDefinitions) {
		this.columnDefs = columnDefinitions;
	}

	/**
	 * 
	 * @param addFilter
	 *            choose to add a field to filter results or not
	 * @param addSelection
	 *            choose to add a column to select some of the displayed results or
	 *            not
	 */
	public void populate(boolean addFilter, boolean addSelection) {
		// initialization
		Composite parent = this;
		hasFilter = addFilter;
		hasSelectionColumn = addSelection;

		// Main Layout
		GridLayout layout = EclipseUiUtils.noSpaceGridLayout();
		layout.verticalSpacing = 5;
		this.setLayout(layout);
		if (hasFilter)
			createFilterPart(parent);

		Composite tableComp = new Composite(parent, SWT.NO_FOCUS);
		tableComp.setLayoutData(EclipseUiUtils.fillAll());
		usersViewer = createTableViewer(tableComp);
		usersViewer.setContentProvider(new UsersContentProvider());
	}

	/**
	 * 
	 * @param showMore
	 *            display static filters on creation
	 * @param addSelection
	 *            choose to add a column to select some of the displayed results or
	 *            not
	 */
	public void populateWithStaticFilters(boolean showMore, boolean addSelection) {
		// initialization
		Composite parent = this;
		hasFilter = true;
		hasSelectionColumn = addSelection;

		// Main Layout
		GridLayout layout = EclipseUiUtils.noSpaceGridLayout();
		layout.verticalSpacing = 5;
		this.setLayout(layout);
		createStaticFilterPart(parent, showMore);

		Composite tableComp = new Composite(parent, SWT.NO_FOCUS);
		tableComp.setLayoutData(EclipseUiUtils.fillAll());
		usersViewer = createTableViewer(tableComp);
		usersViewer.setContentProvider(new UsersContentProvider());
	}

	/** Enable access to the selected users or groups */
	public List<User> getSelectedUsers() {
		if (hasSelectionColumn) {
			Object[] elements = ((CheckboxTableViewer) usersViewer).getCheckedElements();

			List<User> result = new ArrayList<User>();
			for (Object obj : elements) {
				result.add((User) obj);
			}
			return result;
		} else
			throw new EclipseUiException(
					"Unvalid request: no selection column " + "has been created for the current table");
	}

	/** Returns the User table viewer, typically to add doubleclick listener */
	public TableViewer getTableViewer() {
		return usersViewer;
	}

	/**
	 * Force the refresh of the underlying table using the current filter string if
	 * relevant
	 */
	public void refresh() {
		String filter = hasFilter ? filterTxt.getText().trim() : null;
		if ("".equals(filter))
			filter = null;
		refreshFilteredList(filter);
	}

	/** Effective repository request: caller must implement this method */
	abstract protected List<User> listFilteredElements(String filter);

	// protected List<User> listFilteredElements(String filter) {
	// List<User> users = new ArrayList<User>();
	// try {
	// Role[] roles = userAdmin.getRoles(filter);
	// // Display all users and groups
	// for (Role role : roles)
	// users.add((User) role);
	// } catch (InvalidSyntaxException e) {
	// throw new EclipseUiException("Unable to get roles with filter: "
	// + filter, e);
	// }
	// return users;
	// }

	/* GENERIC COMPOSITE METHODS */
	@Override
	public boolean setFocus() {
		if (hasFilter)
			return filterTxt.setFocus();
		else
			return usersViewer.getTable().setFocus();
	}

	@Override
	public void dispose() {
		super.dispose();
	}

	/* LOCAL CLASSES AND METHODS */
	// Will be usefull to rather use a virtual table viewer
	private void refreshFilteredList(String filter) {
		List<User> users = listFilteredElements(filter);
		usersViewer.setInput(users.toArray());
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

	/* MANAGE FILTER */
	private void createFilterPart(Composite parent) {
		// Text Area for the filter
		filterTxt = new Text(parent, SWT.BORDER | SWT.SEARCH | SWT.ICON_SEARCH | SWT.ICON_CANCEL);
		filterTxt.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false));
		filterTxt.addModifyListener(new ModifyListener() {
			private static final long serialVersionUID = 1L;

			public void modifyText(ModifyEvent event) {
				refreshFilteredList(filterTxt.getText());
			}
		});
	}

	private void createStaticFilterPart(Composite parent, boolean showMore) {
		Composite filterComp = new Composite(parent, SWT.NO_FOCUS);
		filterComp.setLayout(new GridLayout(2, false));
		filterComp.setLayoutData(EclipseUiUtils.fillWidth());
		// generic search
		filterTxt = new Text(filterComp, SWT.BORDER | SWT.SEARCH | SWT.ICON_SEARCH | SWT.ICON_CANCEL);
		filterTxt.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false));
		// filterTxt.setLayoutData(new GridData(GridData.GRAB_HORIZONTAL |
		// GridData.HORIZONTAL_ALIGN_FILL));
		filterTxt.addModifyListener(new ModifyListener() {
			private static final long serialVersionUID = 1L;

			public void modifyText(ModifyEvent event) {
				refreshFilteredList(filterTxt.getText());
			}
		});

		// add static filter abilities
		Link moreLk = new Link(filterComp, SWT.NONE);
		Composite staticFilterCmp = new Composite(filterComp, SWT.NO_FOCUS);
		staticFilterCmp.setLayoutData(EclipseUiUtils.fillWidth(2));
		populateStaticFilters(staticFilterCmp);

		MoreLinkListener listener = new MoreLinkListener(moreLk, staticFilterCmp, showMore);
		// initialise the layout
		listener.refresh();
		moreLk.addSelectionListener(listener);
	}

	/** Overwrite to add static filters */
	protected void populateStaticFilters(Composite staticFilterCmp) {
	}

	// private void addMoreSL(final Link more) {
	// more.addSelectionListener( }

	private class MoreLinkListener extends SelectionAdapter {
		private static final long serialVersionUID = -524987616510893463L;
		private boolean isShown;
		private final Composite staticFilterCmp;
		private final Link moreLk;

		public MoreLinkListener(Link moreLk, Composite staticFilterCmp, boolean isShown) {
			this.moreLk = moreLk;
			this.staticFilterCmp = staticFilterCmp;
			this.isShown = isShown;
		}

		@Override
		public void widgetSelected(SelectionEvent e) {
			isShown = !isShown;
			refresh();
		}

		public void refresh() {
			GridData gd = (GridData) staticFilterCmp.getLayoutData();
			if (isShown) {
				moreLk.setText("<a> Less... </a>");
				gd.heightHint = SWT.DEFAULT;
			} else {
				moreLk.setText("<a> More... </a>");
				gd.heightHint = 0;
			}
			forceLayout();
		}
	}

	private void forceLayout() {
		LdifUsersTable.this.getParent().layout(true, true);
	}

	private TableViewer createTableViewer(final Composite parent) {

		int style = tableStyle | SWT.H_SCROLL | SWT.V_SCROLL;
		if (hasSelectionColumn)
			style = style | SWT.CHECK;
		Table table = new Table(parent, style);
		TableColumnLayout layout = new TableColumnLayout();

		// TODO the table layout does not works with the scrolled form

		if (preventTableLayout) {
			parent.setLayout(EclipseUiUtils.noSpaceGridLayout());
			table.setLayoutData(EclipseUiUtils.fillAll());
		} else
			parent.setLayout(layout);

		TableViewer viewer;
		if (hasSelectionColumn)
			viewer = new CheckboxTableViewer(table);
		else
			viewer = new TableViewer(table);
		table.setLinesVisible(true);
		table.setHeaderVisible(true);

		TableViewerColumn column;
		// int offset = 0;
		if (hasSelectionColumn) {
			// offset = 1;
			column = ViewerUtils.createTableViewerColumn(viewer, "", SWT.NONE, 25);
			column.setLabelProvider(new ColumnLabelProvider() {
				private static final long serialVersionUID = 1L;

				@Override
				public String getText(Object element) {
					return null;
				}
			});
			layout.setColumnData(column.getColumn(), new ColumnWeightData(25, 25, false));

			SelectionAdapter selectionAdapter = new SelectionAdapter() {
				private static final long serialVersionUID = 1L;

				boolean allSelected = false;

				@Override
				public void widgetSelected(SelectionEvent e) {
					allSelected = !allSelected;
					((CheckboxTableViewer) usersViewer).setAllChecked(allSelected);
				}
			};
			column.getColumn().addSelectionListener(selectionAdapter);
		}

		// NodeViewerComparator comparator = new NodeViewerComparator();
		// TODO enable the sort by click on the header
		// int i = offset;
		for (ColumnDefinition colDef : columnDefs)
			createTableColumn(viewer, layout, colDef);

		// column = ViewerUtils.createTableViewerColumn(viewer,
		// colDef.getHeaderLabel(), SWT.NONE, colDef.getColumnSize());
		// column.setLabelProvider(new CLProvider(colDef.getPropertyName()));
		// column.getColumn().addSelectionListener(
		// JcrUiUtils.getNodeSelectionAdapter(i,
		// colDef.getPropertyType(), colDef.getPropertyName(),
		// comparator, viewer));
		// i++;
		// }

		// IMPORTANT: initialize comparator before setting it
		// JcrColumnDefinition firstCol = colDefs.get(0);
		// comparator.setColumn(firstCol.getPropertyType(),
		// firstCol.getPropertyName());
		// viewer.setComparator(comparator);

		return viewer;
	}

	/** Default creation of a column for a user table */
	private TableViewerColumn createTableColumn(TableViewer tableViewer, TableColumnLayout layout,
			ColumnDefinition columnDef) {

		boolean resizable = true;
		TableViewerColumn tvc = new TableViewerColumn(tableViewer, SWT.NONE);
		TableColumn column = tvc.getColumn();

		column.setText(columnDef.getLabel());
		column.setWidth(columnDef.getMinWidth());
		column.setResizable(resizable);

		ColumnLabelProvider lp = columnDef.getLabelProvider();
		// add a reference to the display to enable font management
		// if (lp instanceof UserAdminAbstractLP)
		// ((UserAdminAbstractLP) lp).setDisplay(tableViewer.getTable()
		// .getDisplay());
		tvc.setLabelProvider(lp);

		layout.setColumnData(column, new ColumnWeightData(columnDef.getWeight(), columnDef.getMinWidth(), resizable));

		return tvc;
	}
}
