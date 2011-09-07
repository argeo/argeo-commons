package org.argeo.eclipse.ui.jcr.editors;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.QueryResult;
import javax.jcr.query.Row;
import javax.jcr.query.RowIterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.argeo.ArgeoException;
import org.argeo.eclipse.ui.GenericTableComparator;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.EditorPart;

/** Executes any JCR query. */
public abstract class AbstractJcrQueryEditor extends EditorPart {
	private final static Log log = LogFactory
			.getLog(AbstractJcrQueryEditor.class);

	protected String initialQuery;
	protected String initialQueryType;

	/* DEPENDENCY INJECTION */
	private Session session;

	// Widgets
	private TableViewer viewer;
	private List<TableViewerColumn> tableViewerColumns = new ArrayList<TableViewerColumn>();
	private GenericTableComparator comparator;

	/** Override to layout a form enabling the end user to build his query */
	protected abstract void createQueryForm(Composite parent);

	@Override
	public void init(IEditorSite site, IEditorInput input)
			throws PartInitException {
		JcrQueryEditorInput editorInput = (JcrQueryEditorInput) input;
		initialQuery = editorInput.getQuery();
		initialQueryType = editorInput.getQueryType();
		setSite(site);
		setInput(editorInput);
	}

	@Override
	public final void createPartControl(final Composite parent) {
		parent.setLayout(new FillLayout());

		SashForm sashForm = new SashForm(parent, SWT.VERTICAL);
		sashForm.setSashWidth(4);
		sashForm.setLayout(new FillLayout());

		Composite top = new Composite(sashForm, SWT.NONE);
		GridLayout gl = new GridLayout(1, false);
		top.setLayout(gl);

		createQueryForm(top);

		Composite bottom = new Composite(sashForm, SWT.NONE);
		bottom.setLayout(new GridLayout(1, false));
		sashForm.setWeights(getWeights());

		viewer = new TableViewer(bottom);
		viewer.getTable().setLayoutData(
				new GridData(SWT.FILL, SWT.FILL, true, true));
		viewer.getTable().setHeaderVisible(true);
		viewer.setContentProvider(getQueryResultContentProvider());
		viewer.setInput(getEditorSite());

		if (getComparator() != null) {
			comparator = getComparator();
			viewer.setComparator(comparator);
		}
		if (getTableDoubleClickListener() != null)
			viewer.addDoubleClickListener(getTableDoubleClickListener());

	}

	protected void executeQuery(String statement) {
		try {
			if (log.isDebugEnabled())
				log.debug("Query : " + statement);

			QueryResult qr = session.getWorkspace().getQueryManager()
					.createQuery(statement, initialQueryType).execute();

			// remove previous columns
			for (TableViewerColumn tvc : tableViewerColumns)
				tvc.getColumn().dispose();

			int i = 0;
			for (final String columnName : qr.getColumnNames()) {
				TableViewerColumn tvc = new TableViewerColumn(viewer, SWT.NONE);
				configureColumn(columnName, tvc, i);
				tvc.setLabelProvider(getLabelProvider(columnName));
				tableViewerColumns.add(tvc);
				i++;
			}

			// Must create a local list: QueryResults can only be read once.
			try {
				List<Row> rows = new ArrayList<Row>();
				RowIterator rit = qr.getRows();
				while (rit.hasNext()) {
					rows.add(rit.nextRow());
				}
				viewer.setInput(rows);
			} catch (RepositoryException e) {
				throw new ArgeoException("Cannot read query result", e);
			}

		} catch (RepositoryException e) {
			ErrorDialog.openError(null, "Error", "Cannot execute JCR query: "
					+ statement, new Status(IStatus.ERROR,
					"org.argeo.eclipse.ui.jcr", e.getMessage()));
		}
	}

	/**
	 * To be overidden to adapt size of form and result frames.
	 * 
	 * @return
	 */
	protected int[] getWeights() {
		return new int[] { 30, 70 };
	}

	/**
	 * To be overidden to implement a doubleclick Listener on one of the rows of
	 * the table.
	 * 
	 * @return
	 */
	protected IDoubleClickListener getTableDoubleClickListener() {
		return null;
	}

	/**
	 * To be overiden in order to implement a specific
	 * QueryResultContentProvider
	 */
	protected IStructuredContentProvider getQueryResultContentProvider() {
		return new QueryResultContentProvider();
	}

	/**
	 * Enable specific implementation for columns
	 */
	protected List<TableViewerColumn> getTableViewerColumns() {
		return tableViewerColumns;
	}

	/**
	 * Enable specific implementation for columns
	 */
	protected TableViewer getTableViewer() {
		return viewer;
	}

	/**
	 * To be overridden in order to configure column label providers .
	 */
	protected ColumnLabelProvider getLabelProvider(final String columnName) {
		return new ColumnLabelProvider() {
			public String getText(Object element) {
				Row row = (Row) element;
				try {
					return row.getValue(columnName).getString();
				} catch (RepositoryException e) {
					throw new ArgeoException("Cannot display row " + row, e);
				}
			}

			public Image getImage(Object element) {
				return null;
			}
		};
	}

	/**
	 * To be overridden in order to configure the columns.
	 * 
	 * @deprecated use {@link
	 *             org.argeo.eclipse.ui.jcr.editors.AbstractJcrQueryEditor.
	 *             configureColumn(String jcrColumnName, TableViewerColumn
	 *             column, int columnIndex)} instead
	 */
	protected void configureColumn(String jcrColumnName,
			TableViewerColumn column) {
		column.getColumn().setWidth(50);
		column.getColumn().setText(jcrColumnName);
	}

	/** To be overridden in order to configure the columns. */
	protected void configureColumn(String jcrColumnName,
			TableViewerColumn column, int columnIndex) {
		column.getColumn().setWidth(50);
		column.getColumn().setText(jcrColumnName);
	}

	private class QueryResultContentProvider implements
			IStructuredContentProvider {

		public void dispose() {
		}

		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		}

		public Object[] getElements(Object inputElement) {

			if (inputElement instanceof List)
				return ((List) inputElement).toArray();

			// Never reached might be deleted in future release
			if (!(inputElement instanceof QueryResult))
				return new String[] {};

			try {
				QueryResult queryResult = (QueryResult) inputElement;
				List<Row> rows = new ArrayList<Row>();
				RowIterator rit = queryResult.getRows();
				while (rit.hasNext()) {
					rows.add(rit.nextRow());
				}

				// List<Node> elems = new ArrayList<Node>();
				// NodeIterator nit = queryResult.getNodes();
				// while (nit.hasNext()) {
				// elems.add(nit.nextNode());
				// }
				return rows.toArray();
			} catch (RepositoryException e) {
				throw new ArgeoException("Cannot read query result", e);
			}
		}

	}

	/**
	 * Might be used by children classes to sort columns.
	 * 
	 * @param column
	 * @param index
	 * @return
	 */
	protected SelectionAdapter getSelectionAdapter(final TableColumn column,
			final int index) {

		// A comparator must be define
		if (comparator == null)
			return null;

		SelectionAdapter selectionAdapter = new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {

				try {

					comparator.setColumn(index);
					int dir = viewer.getTable().getSortDirection();
					if (viewer.getTable().getSortColumn() == column) {
						dir = dir == SWT.UP ? SWT.DOWN : SWT.UP;
					} else {

						dir = SWT.DOWN;
					}
					viewer.getTable().setSortDirection(dir);
					viewer.getTable().setSortColumn(column);
					viewer.refresh();
				} catch (Exception exc) {
					exc.printStackTrace();
				}
			}
		};
		return selectionAdapter;
	}

	/**
	 * To be overridden to enable sorting.
	 */
	protected GenericTableComparator getComparator() {
		return null;
	}

	@Override
	public boolean isDirty() {
		return false;
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
		// TODO save the query in JCR?
	}

	@Override
	public void doSaveAs() {
	}

	@Override
	public boolean isSaveAsAllowed() {
		return false;
	}

	/** Returns the injected current session */
	protected Session getSession() {
		return session;
	}

	/* DEPENDENCY INJECTION */
	public void setSession(Session session) {
		this.session = session;
	}
}
