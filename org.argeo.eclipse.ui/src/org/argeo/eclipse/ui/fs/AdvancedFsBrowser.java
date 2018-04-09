package org.argeo.eclipse.ui.fs;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.argeo.eclipse.ui.EclipseUiUtils;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Text;

/** Simple UI provider that populates a composite parent given a NIO path */
public class AdvancedFsBrowser {
	private final static Log log = LogFactory.getLog(AdvancedFsBrowser.class);

	// Some local constants to experiment. should be cleaned
	// private final static int THUMBNAIL_WIDTH = 400;
	// private Point imageWidth = new Point(250, 0);
	private final static int COLUMN_WIDTH = 160;

	private Path initialPath;
	private Path currEdited;
	// Filter
	private Composite displayBoxCmp;
	private Text parentPathTxt;
	private Text filterTxt;
	// Browser columns
	private ScrolledComposite scrolledCmp;
	// Keep a cache of the opened directories
	private LinkedHashMap<Path, FilterEntitiesVirtualTable> browserCols = new LinkedHashMap<>();
	private Composite scrolledCmpBody;

	public Control createUi(Composite parent, Path basePath) {
		if (basePath == null)
			throw new IllegalArgumentException("Context cannot be null");
		parent.setLayout(new GridLayout());

		// top filter
		Composite filterCmp = new Composite(parent, SWT.NO_FOCUS);
		filterCmp.setLayoutData(EclipseUiUtils.fillWidth());
		addFilterPanel(filterCmp);

		// Bottom part a sash with browser on the left
		SashForm form = new SashForm(parent, SWT.HORIZONTAL);
		// form.setLayout(new FillLayout());
		form.setLayoutData(EclipseUiUtils.fillAll());
		Composite leftCmp = new Composite(form, SWT.NO_FOCUS);
		displayBoxCmp = new Composite(form, SWT.NONE);
		form.setWeights(new int[] { 3, 1 });

		createBrowserPart(leftCmp, basePath);
		// leftCmp.addControlListener(new ControlAdapter() {
		// @Override
		// public void controlResized(ControlEvent e) {
		// Rectangle r = leftCmp.getClientArea();
		// log.warn("Browser resized: " + r.toString());
		// scrolledCmp.setMinSize(browserCols.size() * (COLUMN_WIDTH + 2),
		// SWT.DEFAULT);
		// // scrolledCmp.setMinSize(scrolledCmpBody.computeSize(SWT.DEFAULT,
		// // r.height));
		// }
		// });

		populateCurrEditedDisplay(displayBoxCmp, basePath);

		// INIT
		setEdited(basePath);
		initialPath = basePath;
		// form.layout(true, true);
		return parent;
	}

	private void createBrowserPart(Composite parent, Path context) {
		parent.setLayout(EclipseUiUtils.noSpaceGridLayout());

		// scrolled composite
		scrolledCmp = new ScrolledComposite(parent, SWT.H_SCROLL | SWT.BORDER | SWT.NO_FOCUS);
		scrolledCmp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		scrolledCmp.setExpandVertical(true);
		scrolledCmp.setExpandHorizontal(true);
		scrolledCmp.setShowFocusedControl(true);

		scrolledCmpBody = new Composite(scrolledCmp, SWT.NO_FOCUS);
		scrolledCmp.setContent(scrolledCmpBody);
		scrolledCmpBody.addControlListener(new ControlAdapter() {
			private static final long serialVersionUID = 183238447102854553L;

			@Override
			public void controlResized(ControlEvent e) {
				Rectangle r = scrolledCmp.getClientArea();
				scrolledCmp.setMinSize(scrolledCmpBody.computeSize(SWT.DEFAULT, r.height));
			}
		});
		initExplorer(scrolledCmpBody, context);
		scrolledCmpBody.layout(true, true);
		scrolledCmp.layout();

	}

	private Control initExplorer(Composite parent, Path context) {
		parent.setLayout(EclipseUiUtils.noSpaceGridLayout());
		return createBrowserColumn(parent, context);
	}

	private Control createBrowserColumn(Composite parent, Path context) {
		// TODO style is not correctly managed.
		FilterEntitiesVirtualTable table = new FilterEntitiesVirtualTable(parent, SWT.BORDER | SWT.NO_FOCUS, context);
		// CmsUtils.style(table, ArgeoOrgStyle.browserColumn.style());
		table.filterList("*");
		table.setLayoutData(new GridData(SWT.LEFT, SWT.FILL, false, true));
		browserCols.put(context, table);
		parent.layout(true, true);
		return table;
	}

	public void addFilterPanel(Composite parent) {
		parent.setLayout(EclipseUiUtils.noSpaceGridLayout(new GridLayout(2, false)));

		parentPathTxt = new Text(parent, SWT.NO_FOCUS);
		parentPathTxt.setEditable(false);

		filterTxt = new Text(parent, SWT.SEARCH | SWT.ICON_CANCEL);
		filterTxt.setMessage("Filter current list");
		filterTxt.setLayoutData(EclipseUiUtils.fillWidth());
		filterTxt.addModifyListener(new ModifyListener() {
			private static final long serialVersionUID = 1L;

			public void modifyText(ModifyEvent event) {
				modifyFilter(false);
			}
		});
		filterTxt.addKeyListener(new KeyListener() {
			private static final long serialVersionUID = 2533535233583035527L;

			@Override
			public void keyReleased(KeyEvent e) {
			}

			@Override
			public void keyPressed(KeyEvent e) {
				boolean shiftPressed = (e.stateMask & SWT.SHIFT) != 0;
				// boolean altPressed = (e.stateMask & SWT.ALT) != 0;
				FilterEntitiesVirtualTable currTable = null;
				if (currEdited != null) {
					FilterEntitiesVirtualTable table = browserCols.get(currEdited);
					if (table != null && !table.isDisposed())
						currTable = table;
				}

				if (e.keyCode == SWT.ARROW_DOWN)
					currTable.setFocus();
				else if (e.keyCode == SWT.BS) {
					if (filterTxt.getText().equals("")
							&& !(currEdited.getNameCount() == 1 || currEdited.equals(initialPath))) {
						Path oldEdited = currEdited;
						Path parentPath = currEdited.getParent();
						setEdited(parentPath);
						if (browserCols.containsKey(parentPath))
							browserCols.get(parentPath).setSelected(oldEdited);
						filterTxt.setFocus();
						e.doit = false;
					}
				} else if (e.keyCode == SWT.TAB && !shiftPressed) {
					Path uniqueChild = getOnlyChild(currEdited, filterTxt.getText());
					if (uniqueChild != null) {
						// Highlight the unique chosen child
						currTable.setSelected(uniqueChild);
						setEdited(uniqueChild);
					}
					filterTxt.setFocus();
					e.doit = false;
				}
			}
		});
	}

	private Path getOnlyChild(Path parent, String filter) {
		try (DirectoryStream<Path> stream = Files.newDirectoryStream(currEdited, filter + "*")) {
			Path uniqueChild = null;
			boolean moreThanOne = false;
			loop: for (Path entry : stream) {
				if (uniqueChild == null) {
					uniqueChild = entry;
				} else {
					moreThanOne = true;
					break loop;
				}
			}
			if (!moreThanOne)
				return uniqueChild;
			return null;
		} catch (IOException ioe) {
			throw new FsUiException(
					"Unable to determine unique child existence and get it under " + parent + " with filter " + filter,
					ioe);
		}
	}

	private void setEdited(Path path) {
		currEdited = path;
		EclipseUiUtils.clear(displayBoxCmp);
		populateCurrEditedDisplay(displayBoxCmp, currEdited);
		refreshFilters(path);
		refreshBrowser(path);
	}

	private void refreshFilters(Path path) {
		parentPathTxt.setText(path.toUri().toString());
		filterTxt.setText("");
		filterTxt.getParent().layout();
	}

	private void refreshBrowser(Path currPath) {
		Path currParPath = currPath.getParent();
		Object[][] colMatrix = new Object[browserCols.size()][2];

		int i = 0, currPathIndex = -1, lastLeftOpenedIndex = -1;
		for (Path path : browserCols.keySet()) {
			colMatrix[i][0] = path;
			colMatrix[i][1] = browserCols.get(path);
			if (currPathIndex >= 0 && lastLeftOpenedIndex < 0 && currParPath != null) {
				boolean leaveOpened = path.startsWith(currPath);
				if (!leaveOpened)
					lastLeftOpenedIndex = i;
			}
			if (currParPath.equals(path))
				currPathIndex = i;
			i++;
		}

		if (currPathIndex >= 0 && lastLeftOpenedIndex >= 0) {
			// dispose and remove useless cols
			for (int l = i - 1; l >= lastLeftOpenedIndex; l--) {
				((FilterEntitiesVirtualTable) colMatrix[l][1]).dispose();
				browserCols.remove(colMatrix[l][0]);
			}
		}

		if (browserCols.containsKey(currPath)) {
			FilterEntitiesVirtualTable currCol = browserCols.get(currPath);
			if (currCol.isDisposed()) {
				// Does it still happen ?
				log.warn(currPath + " browser column was disposed and still listed");
				browserCols.remove(currPath);
			}
		}

		if (!browserCols.containsKey(currPath) && Files.isDirectory(currPath))
			createBrowserColumn(scrolledCmpBody, currPath);

		scrolledCmpBody.setLayout(EclipseUiUtils.noSpaceGridLayout(new GridLayout(browserCols.size(), false)));
		scrolledCmpBody.layout(true, true);
		// also resize the scrolled composite
		scrolledCmp.layout();
	}

	private void modifyFilter(boolean fromOutside) {
		if (!fromOutside)
			if (currEdited != null) {
				String filter = filterTxt.getText() + "*";
				FilterEntitiesVirtualTable table = browserCols.get(currEdited);
				if (table != null && !table.isDisposed())
					table.filterList(filter);
			}
	}

	/**
	 * Recreates the content of the box that displays information about the current
	 * selected node.
	 */
	private void populateCurrEditedDisplay(Composite parent, Path context) {
		parent.setLayout(new GridLayout());

		// if (isImg(context)) {
		// EditableImage image = new Img(parent, RIGHT, context, imageWidth);
		// image.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, false,
		// 2, 1));
		// }

		try {
			Label contextL = new Label(parent, SWT.NONE);
			contextL.setText(context.getFileName().toString());
			contextL.setFont(EclipseUiUtils.getBoldFont(parent));
			addProperty(parent, "Last modified", Files.getLastModifiedTime(context).toString());
			addProperty(parent, "Owner", Files.getOwner(context).getName());
			if (Files.isDirectory(context)) {
				addProperty(parent, "Type", "Folder");
			} else {
				String mimeType = Files.probeContentType(context);
				if (EclipseUiUtils.isEmpty(mimeType))
					mimeType = "<i>Unknown</i>";
				addProperty(parent, "Type", mimeType);
				addProperty(parent, "Size", FsUiUtils.humanReadableByteCount(Files.size(context), false));
			}
			parent.layout(true, true);
		} catch (IOException e) {
			throw new FsUiException("Cannot display details for " + context, e);
		}
	}

	private void addProperty(Composite parent, String propName, String value) {
		Label contextL = new Label(parent, SWT.NONE);
		contextL.setText(propName + ": " + value);
	}

	/**
	 * Almost canonical implementation of a table that displays the content of a
	 * directory
	 */
	private class FilterEntitiesVirtualTable extends Composite {
		private static final long serialVersionUID = 2223410043691844875L;

		// Context
		private Path context;
		private Path currSelected = null;

		// UI Objects
		private FsTableViewer viewer;

		@Override
		public boolean setFocus() {
			if (viewer.getTable().isDisposed())
				return false;
			if (currSelected != null)
				viewer.setSelection(new StructuredSelection(currSelected), true);
			else if (viewer.getSelection().isEmpty()) {
				Object first = viewer.getElementAt(0);
				if (first != null)
					viewer.setSelection(new StructuredSelection(first), true);
			}
			return viewer.getTable().setFocus();
		}

		/**
		 * Enable highlighting the correct element in the table when externally browsing
		 * (typically via the command-line-like Text field)
		 */
		void setSelected(Path selected) {
			// to prevent change selection event to be thrown
			currSelected = selected;
			viewer.setSelection(new StructuredSelection(currSelected), true);
		}

		void filterList(String filter) {
			viewer.setInput(context, filter);
		}

		public FilterEntitiesVirtualTable(Composite parent, int style, Path context) {
			super(parent, SWT.NO_FOCUS);
			this.context = context;
			createTableViewer(this);
		}

		private void createTableViewer(final Composite parent) {
			parent.setLayout(EclipseUiUtils.noSpaceGridLayout());

			// We must limit the size of the table otherwise the full list is
			// loaded before the layout happens
			// Composite listCmp = new Composite(parent, SWT.NO_FOCUS);
			// GridData gd = new GridData(SWT.LEFT, SWT.FILL, false, true);
			// gd.widthHint = COLUMN_WIDTH;
			// listCmp.setLayoutData(gd);
			// listCmp.setLayout(EclipseUiUtils.noSpaceGridLayout());
			// viewer = new TableViewer(listCmp, SWT.VIRTUAL | SWT.MULTI |
			// SWT.V_SCROLL);
			// Table table = viewer.getTable();
			// table.setLayoutData(EclipseUiUtils.fillAll());

			viewer = new FsTableViewer(parent, SWT.MULTI);
			Table table = viewer.configureDefaultSingleColumnTable(COLUMN_WIDTH);

			viewer.addSelectionChangedListener(new ISelectionChangedListener() {

				@Override
				public void selectionChanged(SelectionChangedEvent event) {
					IStructuredSelection selection = (IStructuredSelection) viewer.getSelection();
					if (selection.isEmpty())
						return;
					Object obj = selection.getFirstElement();
					Path newSelected;
					if (obj instanceof Path)
						newSelected = (Path) obj;
					else if (obj instanceof ParentDir)
						newSelected = ((ParentDir) obj).getPath();
					else
						return;
					if (newSelected.equals(currSelected))
						return;
					currSelected = newSelected;
					setEdited(newSelected);

				}
			});

			table.addKeyListener(new KeyListener() {
				private static final long serialVersionUID = -8083424284436715709L;

				@Override
				public void keyReleased(KeyEvent e) {
				}

				@Override
				public void keyPressed(KeyEvent e) {
					IStructuredSelection selection = (IStructuredSelection) viewer.getSelection();
					Path selected = null;
					if (!selection.isEmpty())
						selected = ((Path) selection.getFirstElement());
					if (e.keyCode == SWT.ARROW_RIGHT) {
						if (!Files.isDirectory(selected))
							return;
						if (selected != null) {
							setEdited(selected);
							browserCols.get(selected).setFocus();
						}
					} else if (e.keyCode == SWT.ARROW_LEFT) {
						if (context.equals(initialPath))
							return;
						Path parent = context.getParent();
						if (parent == null)
							return;

						setEdited(parent);
						browserCols.get(parent).setFocus();
					}
				}
			});
		}
	}
}
