package org.argeo.eclipse.ui.jcr.editors;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.QueryResult;
import javax.jcr.query.Row;
import javax.jcr.query.RowIterator;

import org.argeo.ArgeoException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.EditorPart;

/** Executes any JCR query. */
public abstract class AbstractJcrQueryEditor extends EditorPart {
	protected String initialQuery;
	protected String initialQueryType;

	private Session session;

	private TableViewer viewer;
	private List<TableViewerColumn> tableViewerColumns = new ArrayList<TableViewerColumn>();

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
		sashForm.setSashWidth(2);
		sashForm.setLayout(new FillLayout());

		Composite top = new Composite(sashForm, SWT.NONE);
		top.setLayout(new GridLayout(1, false));
		// Device device = Display.getCurrent();
		// Color red = new Color(device, 255, 0, 0);
		// top.setBackground(red);
		createQueryForm(top);

		Composite bottom = new Composite(sashForm, SWT.NONE);
		bottom.setLayout(new GridLayout(1, false));
		sashForm.setWeights(new int[] { 30, 70 });

		viewer = new TableViewer(bottom);
		viewer.getTable().setLayoutData(
				new GridData(SWT.FILL, SWT.FILL, true, true));
		viewer.getTable().setHeaderVisible(true);
		viewer.setContentProvider(new QueryResultContentProvider());
		// viewer.setLabelProvider(new QueryResultLabelProvider());
		viewer.setInput(getEditorSite());
	}

	protected void executeQuery(String statement) {
		try {
			QueryResult qr = session.getWorkspace().getQueryManager()
					.createQuery(statement, initialQueryType).execute();

			// remove previous columns
			for (TableViewerColumn tvc : tableViewerColumns)
				tvc.getColumn().dispose();

			for (final String columnName : qr.getColumnNames()) {
				TableViewerColumn tvc = new TableViewerColumn(viewer, SWT.NONE);
				tvc.getColumn().setWidth(50);
				tvc.getColumn().setText(columnName);
				tvc.setLabelProvider(new ColumnLabelProvider() {

					public String getText(Object element) {
						Row row = (Row) element;
						try {
							return row.getValue(columnName).getString();
						} catch (RepositoryException e) {
							throw new ArgeoException("Cannot display row "
									+ row, e);
						}
					}

					public Image getImage(Object element) {
						return null;
					}
				});
				tableViewerColumns.add(tvc);
			}

			viewer.setInput(qr);
		} catch (RepositoryException e) {
			ErrorDialog.openError(null, "Error", "Cannot execute JCR query: "
					+ statement, new Status(IStatus.ERROR,
					"org.argeo.eclipse.ui.jcr", e.getMessage()));
			// throw new ArgeoException("Cannot execute JCR query " + statement,
			// e);
		}
	}

	private class QueryResultContentProvider implements
			IStructuredContentProvider {

		public void dispose() {
		}

		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		}

		public Object[] getElements(Object inputElement) {
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

	// private class QueryResultLabelProvider extends LabelProvider implements
	// ITableLabelProvider {
	// public String getColumnText(Object element, int columnIndex) {
	// Row row = (Row) element;
	// try {
	// return row.getValues()[columnIndex].toString();
	// } catch (RepositoryException e) {
	// throw new ArgeoException("Cannot display row " + row, e);
	// }
	// }
	//
	// public Image getColumnImage(Object element, int columnIndex) {
	// return null;
	// }
	//
	// }

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

	public void setSession(Session session) {
		this.session = session;
	}

}
