package org.argeo.cms.maintenance;

import static javax.jcr.Node.JCR_CONTENT;
import static org.eclipse.swt.SWT.RIGHT;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.LinkedHashMap;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

import org.argeo.cms.CmsException;
import org.argeo.cms.CmsTypes;
import org.argeo.cms.CmsUiProvider;
import org.argeo.cms.text.Img;
import org.argeo.cms.util.CmsLink;
import org.argeo.cms.util.CmsUtils;
import org.argeo.cms.widgets.EditableImage;
import org.argeo.jcr.JcrUtils;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ILazyContentProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.rap.rwt.RWT;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Text;

public class Browse implements CmsUiProvider {

	// Some local constants to experiment. should be cleaned
	private final static String BROWSE_PREFIX = "browse#";
	private final static int THUMBNAIL_WIDTH = 400;
	private final static int COLUMN_WIDTH = 160;
	private DateFormat timeFormatter = new SimpleDateFormat(
			"dd-MM-yyyy', 'HH:mm");

	// keep a cache of the opened nodes
	// Key is the path
	private LinkedHashMap<String, FilterEntitiesVirtualTable> browserCols = new LinkedHashMap<String, Browse.FilterEntitiesVirtualTable>();
	private Composite nodeDisplayParent;
	private Composite colViewer;
	private ScrolledComposite scrolledCmp;
	private Text parentPathTxt;
	private Text filterTxt;
	private Node currEdited;

	private String initialPath;

	@Override
	public Control createUi(Composite parent, Node context)
			throws RepositoryException {
		if (context == null)
			// return null;
			throw new CmsException("Context cannot be null");
		GridLayout layout = CmsUtils.noSpaceGridLayout();
		layout.numColumns = 2;
		parent.setLayout(layout);

		// Left
		Composite leftCmp = new Composite(parent, SWT.NO_FOCUS);
		leftCmp.setLayoutData(CmsUtils.fillAll());
		createBrowserPart(leftCmp, context);

		// Right
		nodeDisplayParent = new Composite(parent, SWT.NO_FOCUS | SWT.BORDER);
		GridData gd = new GridData(SWT.RIGHT, SWT.FILL, false, true);
		gd.widthHint = THUMBNAIL_WIDTH;
		nodeDisplayParent.setLayoutData(gd);
		createNodeView(nodeDisplayParent, context);

		// INIT
		setEdited(context);
		initialPath = context.getPath();

		// Workaround we don't yet manage the delete to display parent of the
		// initial context node

		return null;
	}

	private void createBrowserPart(Composite parent, Node context)
			throws RepositoryException {
		GridLayout layout = CmsUtils.noSpaceGridLayout();
		parent.setLayout(layout);
		Composite filterCmp = new Composite(parent, SWT.NO_FOCUS);
		filterCmp.setLayoutData(CmsUtils.fillWidth());

		// top filter
		addFilterPanel(filterCmp);

		// scrolled composite
		scrolledCmp = new ScrolledComposite(parent, SWT.H_SCROLL | SWT.BORDER
				| SWT.NO_FOCUS);
		scrolledCmp.setLayoutData(CmsUtils.fillAll());
		scrolledCmp.setExpandVertical(true);
		scrolledCmp.setExpandHorizontal(true);
		scrolledCmp.setShowFocusedControl(true);

		colViewer = new Composite(scrolledCmp, SWT.NO_FOCUS);
		scrolledCmp.setContent(colViewer);
		scrolledCmp.addControlListener(new ControlAdapter() {
			private static final long serialVersionUID = 6589392045145698201L;

			@Override
			public void controlResized(ControlEvent e) {
				Rectangle r = scrolledCmp.getClientArea();
				scrolledCmp.setMinSize(colViewer.computeSize(SWT.DEFAULT,
						r.height));
			}
		});
		initExplorer(colViewer, context);
	}

	private Control initExplorer(Composite parent, Node context)
			throws RepositoryException {
		parent.setLayout(CmsUtils.noSpaceGridLayout());
		createBrowserColumn(parent, context);
		return null;
	}

	private Control createBrowserColumn(Composite parent, Node context)
			throws RepositoryException {
		// TODO style is not correctly managed.
		FilterEntitiesVirtualTable table = new FilterEntitiesVirtualTable(
				parent, SWT.BORDER | SWT.NO_FOCUS, context);
		// CmsUtils.style(table, ArgeoOrgStyle.browserColumn.style());
		table.filterList("*");
		table.setLayoutData(new GridData(SWT.LEFT, SWT.FILL, false, true));
		browserCols.put(context.getPath(), table);
		return null;
	}

	public void addFilterPanel(Composite parent) {

		parent.setLayout(CmsUtils.noSpaceGridLayout(new GridLayout(2, false)));

		// Text Area for the filter
		parentPathTxt = new Text(parent, SWT.NO_FOCUS);
		parentPathTxt.setEditable(false);
		filterTxt = new Text(parent, SWT.SEARCH | SWT.ICON_CANCEL);
		filterTxt.setMessage("Filter current list");
		filterTxt.setLayoutData(CmsUtils.fillWidth());
		filterTxt.addModifyListener(new ModifyListener() {
			private static final long serialVersionUID = 7709303319740056286L;

			public void modifyText(ModifyEvent event) {
				modifyFilter(false);
			}
		});

		filterTxt.addKeyListener(new KeyListener() {
			private static final long serialVersionUID = -4523394262771183968L;

			@Override
			public void keyReleased(KeyEvent e) {
			}

			@Override
			public void keyPressed(KeyEvent e) {
				boolean shiftPressed = (e.stateMask & SWT.SHIFT) != 0;
				// boolean altPressed = (e.stateMask & SWT.ALT) != 0;
				FilterEntitiesVirtualTable currTable = null;
				if (currEdited != null) {
					FilterEntitiesVirtualTable table = browserCols
							.get(getPath(currEdited));
					if (table != null && !table.isDisposed())
						currTable = table;
				}

				try {
					if (e.keyCode == SWT.ARROW_DOWN)
						currTable.setFocus();
					else if (e.keyCode == SWT.BS) {
						if (filterTxt.getText().equals("")
								&& !(getPath(currEdited).equals("/") || getPath(
										currEdited).equals(initialPath))) {
							setEdited(currEdited.getParent());
							e.doit = false;
							filterTxt.setFocus();
						}
					} else if (e.keyCode == SWT.TAB && !shiftPressed) {
						if (currEdited.getNodes(filterTxt.getText() + "*")
								.getSize() == 1) {
							setEdited(currEdited.getNodes(
									filterTxt.getText() + "*").nextNode());
						}
						filterTxt.setFocus();
						e.doit = false;
					}
				} catch (RepositoryException e1) {
					throw new CmsException(
							"Unexpected error in key management for "
									+ currEdited + "with filter "
									+ filterTxt.getText(), e1);
				}

			}
		});
	}

	private void setEdited(Node node) {
		try {
			currEdited = node;
			CmsUtils.clear(nodeDisplayParent);
			createNodeView(nodeDisplayParent, currEdited);
			nodeDisplayParent.layout();
			refreshFilters(node);
			refreshBrowser(node);
		} catch (RepositoryException re) {
			throw new CmsException("Unable to update browser for " + node, re);
		}
	}

	private void refreshFilters(Node node) throws RepositoryException {
		String currNodePath = node.getPath();
		parentPathTxt.setText(currNodePath);
		filterTxt.setText("");
		filterTxt.getParent().layout();
	}

	private void refreshBrowser(Node node) throws RepositoryException {

		// Retrieve
		String currNodePath = node.getPath();
		String currParPath = "";
		if (!"/".equals(currNodePath))
			currParPath = JcrUtils.parentPath(currNodePath);
		if ("".equals(currParPath))
			currParPath = "/";
		
		
		

		Object[][] colMatrix = new Object[browserCols.size()][2];

		int i = 0, j = -1, k = -1;
		for (String path : browserCols.keySet()) {
			colMatrix[i][0] = path;
			colMatrix[i][1] = browserCols.get(path);
			if (j >= 0 && k < 0 && !currNodePath.equals("/")) {
				boolean leaveOpened = path.startsWith(currNodePath);

				// workaround for same name siblings
				// fix me weird side effect when we go left or click on anb
				// already selected, unfocused node
				if (leaveOpened
						&& (path.lastIndexOf("/") == 0
								&& currNodePath.lastIndexOf("/") == 0 || JcrUtils
								.parentPath(path).equals(
										JcrUtils.parentPath(currNodePath))))
					leaveOpened = JcrUtils.lastPathElement(path).equals(
							JcrUtils.lastPathElement(currNodePath));

				if (!leaveOpened)
					k = i;
			}
			if (currParPath.equals(path))
				j = i;
			i++;
		}

		if (j >= 0 && k >= 0)
			// remove useless cols
			for (int l = i - 1; l >= k; l--) {
				browserCols.remove(colMatrix[l][0]);
				((FilterEntitiesVirtualTable) colMatrix[l][1]).dispose();
			}

		// Remove disposed columns
		// TODO investigate and fix the mechanism that leave them there after
		// disposal
		if (browserCols.containsKey(currNodePath)) {
			FilterEntitiesVirtualTable currCol = browserCols.get(currNodePath);
			if (currCol.isDisposed())
				browserCols.remove(currNodePath);
		}

		if (!browserCols.containsKey(currNodePath))
			createBrowserColumn(colViewer, node);

		colViewer.setLayout(CmsUtils.noSpaceGridLayout(new GridLayout(
				browserCols.size(), false)));
		// colViewer.pack();
		colViewer.layout();
		// also resize the scrolled composite
		scrolledCmp.layout();
		scrolledCmp.getShowFocusedControl();
		// colViewer.getParent().layout();
		// if (JcrUtils.parentPath(currNodePath).equals(currBrowserKey)) {
		// } else {
		// }
	}

	private void modifyFilter(boolean fromOutside) {
		if (!fromOutside)
			if (currEdited != null) {
				String filter = filterTxt.getText() + "*";
				FilterEntitiesVirtualTable table = browserCols
						.get(getPath(currEdited));
				if (table != null && !table.isDisposed())
					table.filterList(filter);
			}

	}

	private String getPath(Node node) {
		try {
			return node.getPath();
		} catch (RepositoryException e) {
			throw new CmsException("Unable to get path for node " + node, e);
		}
	}

	private Point imageWidth = new Point(250, 0);

	/**
	 * Recreates the content of the box that displays information about the
	 * current selected node.
	 */
	private Control createNodeView(Composite parent, Node context)
			throws RepositoryException {

		parent.setLayout(new GridLayout(2, false));

		if (isImg(context)) {
			EditableImage image = new Img(parent, RIGHT, context, imageWidth);
			image.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true,
					false, 2, 1));
		}

		// Name and primary type
		Label contextL = new Label(parent, SWT.NONE);
		contextL.setData(RWT.MARKUP_ENABLED, true);
		contextL.setText("<b>" + context.getName() + "</b>");
		new Label(parent, SWT.NONE).setText(context.getPrimaryNodeType()
				.getName());

		// Children
		for (NodeIterator nIt = context.getNodes(); nIt.hasNext();) {
			Node child = nIt.nextNode();
			new CmsLink(child.getName(), BROWSE_PREFIX + child.getPath())
					.createUi(parent, context);
			new Label(parent, SWT.NONE).setText(child.getPrimaryNodeType()
					.getName());
		}

		// Properties
		for (PropertyIterator pIt = context.getProperties(); pIt.hasNext();) {
			Property property = pIt.nextProperty();
			Label label = new Label(parent, SWT.NONE);
			label.setText(property.getName());
			label.setToolTipText(JcrUtils
					.getPropertyDefinitionAsString(property));
			new Label(parent, SWT.NONE).setText(getPropAsString(property));
		}

		return null;
	}

	private boolean isImg(Node node) throws RepositoryException {
		return node.hasNode(JCR_CONTENT) && node.isNodeType(CmsTypes.CMS_IMAGE);
	}

	private String getPropAsString(Property property)
			throws RepositoryException {
		String result = "";
		if (property.isMultiple()) {
			result = getMultiAsString(property, ", ");
		} else {
			Value value = property.getValue();
			if (value.getType() == PropertyType.BINARY)
				result = "<binary>";
			else if (value.getType() == PropertyType.DATE)
				result = timeFormatter.format(value.getDate().getTime());
			else
				result = value.getString();
		}
		return result;
	}

	private String getMultiAsString(Property property, String separator)
			throws RepositoryException {
		if (separator == null)
			separator = "; ";
		Value[] values = property.getValues();
		StringBuilder builder = new StringBuilder();
		for (Value val : values) {
			String currStr = val.getString();
			if (!"".equals(currStr.trim()))
				builder.append(currStr).append(separator);
		}
		if (builder.lastIndexOf(separator) >= 0)
			return builder.substring(0, builder.length() - separator.length());
		else
			return builder.toString();
	}

	/** Almost canonical implementation of a table that display entities */
	private class FilterEntitiesVirtualTable extends Composite {
		private static final long serialVersionUID = 8798147431706283824L;

		// Context
		private Node context;

		// UI Objects
		private TableViewer entityViewer;

		// enable management of multiple columns
		Node getNode() {
			return context;
		}

		@Override
		public boolean setFocus() {
			if (entityViewer.getTable().isDisposed())
				return false;
			if (entityViewer.getSelection().isEmpty()) {
				Object first = entityViewer.getElementAt(0);
				if (first != null) {
					entityViewer.setSelection(new StructuredSelection(first),
							true);
				}
			}
			return entityViewer.getTable().setFocus();
		}

		void filterList(String filter) {
			try {
				NodeIterator nit = context.getNodes(filter);
				refreshFilteredList(nit);
			} catch (RepositoryException e) {
				throw new CmsException("Unable to filter " + getNode()
						+ " children with filter " + filter, e);
			}

		}

		public FilterEntitiesVirtualTable(Composite parent, int style,
				Node context) {
			super(parent, SWT.NO_FOCUS);
			this.context = context;
			populate();
		}

		protected void populate() {
			Composite parent = this;
			GridLayout layout = CmsUtils.noSpaceGridLayout();

			this.setLayout(layout);
			createTableViewer(parent);
		}

		private void createTableViewer(final Composite parent) {
			// the list
			// We must limit the size of the table otherwise the full list is
			// loaded
			// before the layout happens
			Composite listCmp = new Composite(parent, SWT.NO_FOCUS);
			GridData gd = new GridData(SWT.LEFT, SWT.FILL, false, true);
			gd.widthHint = COLUMN_WIDTH;
			listCmp.setLayoutData(gd);
			listCmp.setLayout(CmsUtils.noSpaceGridLayout());

			entityViewer = new TableViewer(listCmp, SWT.VIRTUAL | SWT.SINGLE);
			Table table = entityViewer.getTable();

			table.setLayoutData(CmsUtils.fillAll());
			table.setLinesVisible(true);
			table.setHeaderVisible(false);
			table.setData(RWT.MARKUP_ENABLED, Boolean.TRUE);

			CmsUtils.style(table, MaintenanceStyles.BROWSER_COLUMN);

			// first column
			TableViewerColumn column = new TableViewerColumn(entityViewer,
					SWT.NONE);
			TableColumn tcol = column.getColumn();
			tcol.setWidth(COLUMN_WIDTH);
			tcol.setResizable(true);
			column.setLabelProvider(new SimpleNameLP());

			entityViewer.setContentProvider(new MyLazyCP(entityViewer));
			entityViewer
					.addSelectionChangedListener(new ISelectionChangedListener() {

						@Override
						public void selectionChanged(SelectionChangedEvent event) {
							IStructuredSelection selection = (IStructuredSelection) entityViewer
									.getSelection();
							if (selection.isEmpty())
								return;
							else
								setEdited((Node) selection.getFirstElement());

						}
					});

			table.addKeyListener(new KeyListener() {
				private static final long serialVersionUID = -330694313896036230L;

				@Override
				public void keyReleased(KeyEvent e) {
				}

				@Override
				public void keyPressed(KeyEvent e) {

					IStructuredSelection selection = (IStructuredSelection) entityViewer
							.getSelection();
					Node selected = null;
					if (!selection.isEmpty())
						selected = ((Node) selection.getFirstElement());
					try {
						if (e.keyCode == SWT.ARROW_RIGHT) {
							if (selected != null) {
								setEdited(selected);
								browserCols.get(selected.getPath()).setFocus();
							}
						} else if (e.keyCode == SWT.ARROW_LEFT) {
							try {
								selected = getNode().getParent();
								String newPath = selected.getPath(); // getNode().getParent()
								setEdited(selected);
								if (browserCols.containsKey(newPath))
									browserCols.get(newPath).setFocus();
							} catch (ItemNotFoundException ie) {
								// root silent
							}
						}
					} catch (RepositoryException ie) {
						throw new CmsException("Error while managing arrow "
								+ "events in the browser for " + selected, ie);
					}
				}
			});
		}

		private class MyLazyCP implements ILazyContentProvider {
			private static final long serialVersionUID = 1L;
			private TableViewer viewer;
			private Object[] elements;

			public MyLazyCP(TableViewer viewer) {
				this.viewer = viewer;
			}

			public void dispose() {
			}

			public void inputChanged(Viewer viewer, Object oldInput,
					Object newInput) {
				// IMPORTANT: don't forget this: an exception will be thrown if
				// a selected object is not part of the results anymore.
				viewer.setSelection(null);
				this.elements = (Object[]) newInput;
			}

			public void updateElement(int index) {
				viewer.replace(elements[index], index);
			}
		}

		protected void refreshFilteredList(NodeIterator children) {
			Object[] rows = JcrUtils.nodeIteratorToList(children).toArray();
			entityViewer.setInput(rows);
			entityViewer.setItemCount(rows.length);
			entityViewer.refresh();
		}

		public class SimpleNameLP extends ColumnLabelProvider {
			private static final long serialVersionUID = 2465059387875338553L;

			@Override
			public String getText(Object element) {
				if (element instanceof Node) {
					Node curr = ((Node) element);
					try {
						return curr.getName();
					} catch (RepositoryException e) {
						throw new CmsException("Unable to get name for"
								+ curr);
					}
				}
				return super.getText(element);
			}
		}
	}
}