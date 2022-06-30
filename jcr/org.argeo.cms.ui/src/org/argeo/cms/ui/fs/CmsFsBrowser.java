package org.argeo.cms.ui.fs;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.spi.FileSystemProvider;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.Session;

import org.argeo.cms.auth.CurrentUser;
import org.argeo.cms.jcr.CmsJcrUtils;
import org.argeo.cms.swt.CmsException;
import org.argeo.cms.swt.CmsSwtUtils;
import org.argeo.eclipse.ui.ColumnDefinition;
import org.argeo.eclipse.ui.EclipseUiUtils;
import org.argeo.eclipse.ui.fs.FileIconNameLabelProvider;
import org.argeo.eclipse.ui.fs.FsTableViewer;
import org.argeo.eclipse.ui.fs.FsUiConstants;
import org.argeo.eclipse.ui.fs.FsUiUtils;
import org.argeo.eclipse.ui.fs.NioFileLabelProvider;
import org.argeo.jcr.JcrUtils;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Text;

/**
 * Default CMS browser composite: a sashForm layout with bookmarks at the left
 * hand side, a simple table in the middle and an overview at right hand side.
 */
public class CmsFsBrowser extends Composite {
	// private final static Log log = LogFactory.getLog(CmsFsBrowser.class);
	private static final long serialVersionUID = -40347919096946585L;

	private final FileSystemProvider nodeFileSystemProvider;
	private final Node currentBaseContext;

	// UI Parts for the browser
	private Composite leftPannelCmp;
	private Composite filterCmp;
	private Text filterTxt;
	private FsTableViewer directoryDisplayViewer;
	private Composite rightPannelCmp;

	private FsContextMenu contextMenu;

	// Local context (this composite is state full)
	private Path initialPath;
	private Path currDisplayedFolder;
	private Path currSelected;

	// local variables (to be cleaned)
	private int bookmarkColWith = 500;

	/*
	 * WARNING: unfinalised implementation of the mechanism to retrieve base
	 * paths
	 */

	private final static String NODE_PREFIX = "node://";

	private String getCurrentHomePath() {
		Session session = null;
		try {
			Repository repo = currentBaseContext.getSession().getRepository();
			session = CurrentUser.tryAs(() -> repo.login());
			String homepath = CmsJcrUtils.getUserHome(session).getPath();
			return homepath;
		} catch (Exception e) {
			throw new CmsException("Cannot retrieve Current User Home Path", e);
		} finally {
			JcrUtils.logoutQuietly(session);
		}
	}

	protected Path[] getMyFilesPath() {
		// return Paths.get(System.getProperty("user.dir"));
		String currHomeUriStr = NODE_PREFIX + getCurrentHomePath();
		try {
			URI uri = new URI(currHomeUriStr);
			FileSystem fileSystem = nodeFileSystemProvider.getFileSystem(uri);
			if (fileSystem == null) {
				PrivilegedExceptionAction<FileSystem> pea = new PrivilegedExceptionAction<FileSystem>() {
					@Override
					public FileSystem run() throws Exception {
						return nodeFileSystemProvider.newFileSystem(uri, null);
					}

				};
				fileSystem = CurrentUser.tryAs(pea);
			}
			Path[] paths = { fileSystem.getPath(getCurrentHomePath()), fileSystem.getPath("/") };
			return paths;
		} catch (URISyntaxException | PrivilegedActionException e) {
			throw new RuntimeException("unable to initialise home file system for " + currHomeUriStr, e);
		}
	}

	private Path[] getMyGroupsFilesPath() {
		// TODO
		Path[] paths = { Paths.get(System.getProperty("user.dir")), Paths.get("/tmp") };
		return paths;
	}

	private Path[] getMyBookmarks() {
		// TODO
		Path[] paths = { Paths.get(System.getProperty("user.dir")), Paths.get("/tmp"), Paths.get("/opt") };
		return paths;
	}

	/* End of warning */

	public CmsFsBrowser(Composite parent, int style, Node context, FileSystemProvider fileSystemProvider) {
		super(parent, style);
		this.nodeFileSystemProvider = fileSystemProvider;
		this.currentBaseContext = context;

		this.setLayout(EclipseUiUtils.noSpaceGridLayout());

		SashForm form = new SashForm(this, SWT.HORIZONTAL);

		leftPannelCmp = new Composite(form, SWT.NO_FOCUS);
		// Bookmarks are still static
		populateBookmarks(leftPannelCmp);

		Composite centerCmp = new Composite(form, SWT.BORDER | SWT.NO_FOCUS);
		createDisplay(centerCmp);

		rightPannelCmp = new Composite(form, SWT.NO_FOCUS);

		form.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		form.setWeights(new int[] { 15, 40, 20 });
	}

	void refresh() {
		modifyFilter(false);
		// also refresh bookmarks and groups
	}

	private void createDisplay(final Composite parent) {
		parent.setLayout(EclipseUiUtils.noSpaceGridLayout());

		// top filter
		filterCmp = new Composite(parent, SWT.NO_FOCUS);
		filterCmp.setLayoutData(EclipseUiUtils.fillWidth());
		addFilterPanel(filterCmp);

		// Main display
		directoryDisplayViewer = new FsTableViewer(parent, SWT.MULTI);
		List<ColumnDefinition> colDefs = new ArrayList<>();
		colDefs.add(new ColumnDefinition(new FileIconNameLabelProvider(), "Name", 250));
		colDefs.add(new ColumnDefinition(new NioFileLabelProvider(FsUiConstants.PROPERTY_SIZE), "Size", 100));
		colDefs.add(new ColumnDefinition(new NioFileLabelProvider(FsUiConstants.PROPERTY_TYPE), "Type", 150));
		colDefs.add(new ColumnDefinition(new NioFileLabelProvider(FsUiConstants.PROPERTY_LAST_MODIFIED),
				"Last modified", 400));
		final Table table = directoryDisplayViewer.configureDefaultTable(colDefs);
		table.setLayoutData(EclipseUiUtils.fillAll());

		// table.addKeyListener(new KeyListener() {
		// private static final long serialVersionUID = -8083424284436715709L;
		//
		// @Override
		// public void keyReleased(KeyEvent e) {
		// }
		//
		// @Override
		// public void keyPressed(KeyEvent e) {
		// if (log.isDebugEnabled())
		// log.debug("Key event received: " + e.keyCode);
		// IStructuredSelection selection = (IStructuredSelection)
		// directoryDisplayViewer.getSelection();
		// Path selected = null;
		// if (!selection.isEmpty())
		// selected = ((Path) selection.getFirstElement());
		// if (e.keyCode == SWT.CR) {
		// if (!Files.isDirectory(selected))
		// return;
		// if (selected != null) {
		// currDisplayedFolder = selected;
		// directoryDisplayViewer.setInput(currDisplayedFolder, "*");
		// }
		// } else if (e.keyCode == SWT.BS) {
		// currDisplayedFolder = currDisplayedFolder.getParent();
		// directoryDisplayViewer.setInput(currDisplayedFolder, "*");
		// directoryDisplayViewer.getTable().setFocus();
		// }
		// }
		// });

		directoryDisplayViewer.addSelectionChangedListener(new ISelectionChangedListener() {

			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				IStructuredSelection selection = (IStructuredSelection) directoryDisplayViewer.getSelection();
				Path selected = null;
				if (selection.isEmpty())
					setSelected(null);
				else
					selected = ((Path) selection.getFirstElement());
				if (selected != null) {
					// TODO manage multiple selection
					setSelected(selected);
				}
			}
		});

		directoryDisplayViewer.addDoubleClickListener(new IDoubleClickListener() {
			@Override
			public void doubleClick(DoubleClickEvent event) {
				IStructuredSelection selection = (IStructuredSelection) directoryDisplayViewer.getSelection();
				Path selected = null;
				if (!selection.isEmpty())
					selected = ((Path) selection.getFirstElement());
				if (selected != null) {
					if (!Files.isDirectory(selected))
						return;
					setInput(selected);
				}
			}
		});

		// The context menu
		contextMenu = new FsContextMenu(this);

		table.addMouseListener(new MouseAdapter() {
			private static final long serialVersionUID = 6737579410648595940L;

			@Override
			public void mouseDown(MouseEvent e) {
				if (e.button == 3) {
					// contextMenu.setCurrFolderPath(currDisplayedFolder);
					contextMenu.show(table, new Point(e.x, e.y), currDisplayedFolder);
				}
			}
		});
	}

	private void addPathElementBtn(Path path) {
		Button elemBtn = new Button(filterCmp, SWT.PUSH);
		String nameStr;
		if (path.toString().equals("/"))
			nameStr = "[jcr:root]";
		else
			nameStr = path.getFileName().toString();
		elemBtn.setText(nameStr + " >> ");
		CmsSwtUtils.style(elemBtn, FsStyles.BREAD_CRUMB_BTN);
		elemBtn.addSelectionListener(new SelectionAdapter() {
			private static final long serialVersionUID = -4103695476023480651L;

			@Override
			public void widgetSelected(SelectionEvent e) {
				setInput(path);
			}
		});
	}

	public void setInput(Path path) {
		if (path.equals(currDisplayedFolder))
			return;
		currDisplayedFolder = path;

		Path diff = initialPath.relativize(currDisplayedFolder);

		for (Control child : filterCmp.getChildren())
			if (!child.equals(filterTxt))
				child.dispose();

		addPathElementBtn(initialPath);
		Path currTarget = initialPath;
		if (!diff.toString().equals(""))
			for (Path pathElem : diff) {
				currTarget = currTarget.resolve(pathElem);
				addPathElementBtn(currTarget);
			}

		filterTxt.setText("");
		filterTxt.moveBelow(null);
		setSelected(null);
		filterCmp.getParent().layout(true, true);
	}

	private void setSelected(Path path) {
		currSelected = path;
		setOverviewInput(path);
	}

	public Viewer getViewer() {
		return directoryDisplayViewer;
	}

	private void populateBookmarks(Composite parent) {
		CmsSwtUtils.clear(parent);
		parent.setLayout(new GridLayout());
		ISelectionChangedListener selList = new BookmarksSelChangeListener();

		FsTableViewer homeViewer = new FsTableViewer(parent, SWT.SINGLE | SWT.NO_SCROLL);
		Table table = homeViewer.configureDefaultSingleColumnTable(bookmarkColWith);
		GridData gd = EclipseUiUtils.fillWidth();
		gd.horizontalIndent = 10;
		table.setLayoutData(gd);
		homeViewer.addSelectionChangedListener(selList);
		homeViewer.setPathsInput(getMyFilesPath());

		appendTitle(parent, "Shared files");
		FsTableViewer groupsViewer = new FsTableViewer(parent, SWT.SINGLE | SWT.NO_SCROLL);
		table = groupsViewer.configureDefaultSingleColumnTable(bookmarkColWith);
		gd = EclipseUiUtils.fillWidth();
		gd.horizontalIndent = 10;
		table.setLayoutData(gd);
		groupsViewer.addSelectionChangedListener(selList);
		groupsViewer.setPathsInput(getMyGroupsFilesPath());

		appendTitle(parent, "My bookmarks");
		FsTableViewer bookmarksViewer = new FsTableViewer(parent, SWT.SINGLE | SWT.NO_SCROLL);
		table = bookmarksViewer.configureDefaultSingleColumnTable(bookmarkColWith);
		gd = EclipseUiUtils.fillWidth();
		gd.horizontalIndent = 10;
		table.setLayoutData(gd);
		bookmarksViewer.addSelectionChangedListener(selList);
		bookmarksViewer.setPathsInput(getMyBookmarks());
	}

	/**
	 * Recreates the content of the box that displays information about the
	 * current selected Path.
	 */
	private void setOverviewInput(Path path) {
		try {
			EclipseUiUtils.clear(rightPannelCmp);
			rightPannelCmp.setLayout(new GridLayout());
			if (path != null) {
				// if (isImg(context)) {
				// EditableImage image = new Img(parent, RIGHT, context,
				// imageWidth);
				// image.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER,
				// true, false,
				// 2, 1));
				// }

				Label contextL = new Label(rightPannelCmp, SWT.NONE);
				contextL.setText(path.getFileName().toString());
				contextL.setFont(EclipseUiUtils.getBoldFont(rightPannelCmp));
				addProperty(rightPannelCmp, "Last modified", Files.getLastModifiedTime(path).toString());
				// addProperty(rightPannelCmp, "Owner",
				// Files.getOwner(path).getName());
				if (Files.isDirectory(path)) {
					addProperty(rightPannelCmp, "Type", "Folder");
				} else {
					String mimeType = Files.probeContentType(path);
					if (EclipseUiUtils.isEmpty(mimeType))
						mimeType = "<i>Unknown</i>";
					addProperty(rightPannelCmp, "Type", mimeType);
					addProperty(rightPannelCmp, "Size", FsUiUtils.humanReadableByteCount(Files.size(path), false));
				}
			}
			rightPannelCmp.layout(true, true);
		} catch (IOException e) {
			throw new CmsException("Cannot display details for " + path.toString(), e);
		}
	}

	private void addFilterPanel(Composite parent) {
		RowLayout rl = new RowLayout(SWT.HORIZONTAL);
		rl.wrap = true;
		parent.setLayout(rl);
		// parent.setLayout(EclipseUiUtils.noSpaceGridLayout(new GridLayout(2,
		// false)));

		filterTxt = new Text(parent, SWT.SEARCH | SWT.ICON_CANCEL);
		filterTxt.setMessage("Search current folder");
		filterTxt.setLayoutData(new RowData(250, SWT.DEFAULT));
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
				// boolean shiftPressed = (e.stateMask & SWT.SHIFT) != 0;
				// // boolean altPressed = (e.stateMask & SWT.ALT) != 0;
				// FilterEntitiesVirtualTable currTable = null;
				// if (currEdited != null) {
				// FilterEntitiesVirtualTable table =
				// browserCols.get(currEdited);
				// if (table != null && !table.isDisposed())
				// currTable = table;
				// }
				//
				// if (e.keyCode == SWT.ARROW_DOWN)
				// currTable.setFocus();
				// else if (e.keyCode == SWT.BS) {
				// if (filterTxt.getText().equals("")
				// && !(currEdited.getNameCount() == 1 ||
				// currEdited.equals(initialPath))) {
				// Path oldEdited = currEdited;
				// Path parentPath = currEdited.getParent();
				// setEdited(parentPath);
				// if (browserCols.containsKey(parentPath))
				// browserCols.get(parentPath).setSelected(oldEdited);
				// filterTxt.setFocus();
				// e.doit = false;
				// }
				// } else if (e.keyCode == SWT.TAB && !shiftPressed) {
				// Path uniqueChild = getOnlyChild(currEdited,
				// filterTxt.getText());
				// if (uniqueChild != null) {
				// // Highlight the unique chosen child
				// currTable.setSelected(uniqueChild);
				// setEdited(uniqueChild);
				// }
				// filterTxt.setFocus();
				// e.doit = false;
				// }
			}
		});
	}

	private Path getOnlyChild(Path parent, String filter) {
		try (DirectoryStream<Path> stream = Files.newDirectoryStream(currDisplayedFolder, filter + "*")) {
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
			throw new CmsException(
					"Unable to determine unique child existence and get it under " + parent + " with filter " + filter,
					ioe);
		}
	}

	private void modifyFilter(boolean fromOutside) {
		if (!fromOutside)
			if (currDisplayedFolder != null) {
				String filter = filterTxt.getText() + "*";
				directoryDisplayViewer.setInput(currDisplayedFolder, filter);
			}
	}

	private class BookmarksSelChangeListener implements ISelectionChangedListener {

		@Override
		public void selectionChanged(SelectionChangedEvent event) {
			IStructuredSelection selection = (IStructuredSelection) event.getSelection();
			if (selection.isEmpty())
				return;
			else {
				Path newSelected = (Path) selection.getFirstElement();
				if (newSelected.equals(currDisplayedFolder) && newSelected.equals(initialPath))
					return;
				initialPath = newSelected;
				setInput(newSelected);
			}
		}
	}

	// Simplify UI implementation
	private void addProperty(Composite parent, String propName, String value) {
		Label contextL = new Label(parent, SWT.NONE);
		contextL.setText(propName + ": " + value);
	}

	private Label appendTitle(Composite parent, String value) {
		Label titleLbl = new Label(parent, SWT.NONE);
		titleLbl.setText(value);
		titleLbl.setFont(EclipseUiUtils.getBoldFont(parent));
		GridData gd = EclipseUiUtils.fillWidth();
		gd.horizontalIndent = 5;
		gd.verticalIndent = 5;
		titleLbl.setLayoutData(gd);
		return titleLbl;
	}
}
