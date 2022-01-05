package org.argeo.cms.ui.fs;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.argeo.api.cms.CmsLog;
import org.argeo.cms.CmsException;
import org.argeo.cms.swt.CmsSwtUtils;
import org.argeo.eclipse.ui.EclipseUiUtils;
import org.argeo.eclipse.ui.dialogs.SingleValue;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

/** Generic popup context menu to manage NIO Path in a Viewer. */
public class FsContextMenu extends Shell {
	private static final long serialVersionUID = -9120261153509855795L;

	private final static CmsLog log = CmsLog.getLog(FsContextMenu.class);

	// Default known actions
	public final static String ACTION_ID_CREATE_FOLDER = "createFolder";
	public final static String ACTION_ID_BOOKMARK_FOLDER = "bookmarkFolder";
	public final static String ACTION_ID_SHARE_FOLDER = "shareFolder";
	public final static String ACTION_ID_DOWNLOAD_FOLDER = "downloadFolder";
	public final static String ACTION_ID_DELETE = "delete";
	public final static String ACTION_ID_UPLOAD_FILE = "uploadFiles";
	public final static String ACTION_ID_OPEN = "open";

	// Local context
	private final CmsFsBrowser browser;
	// private final Viewer viewer;
	private final static String KEY_ACTION_ID = "actionId";
	private final static String[] DEFAULT_ACTIONS = { ACTION_ID_CREATE_FOLDER, ACTION_ID_BOOKMARK_FOLDER,
			ACTION_ID_SHARE_FOLDER, ACTION_ID_DOWNLOAD_FOLDER, ACTION_ID_DELETE, ACTION_ID_UPLOAD_FILE,
			ACTION_ID_OPEN };
	private Map<String, Button> actionButtons = new HashMap<String, Button>();

	private Path currFolderPath;

	public FsContextMenu(CmsFsBrowser browser) { // Viewer viewer, Display
													// display) {
		super(browser.getDisplay(), SWT.NO_TRIM | SWT.BORDER | SWT.ON_TOP);
		this.browser = browser;
		setLayout(EclipseUiUtils.noSpaceGridLayout());

		Composite boxCmp = new Composite(this, SWT.NO_FOCUS | SWT.BORDER);
		boxCmp.setLayout(EclipseUiUtils.noSpaceGridLayout());
		CmsSwtUtils.style(boxCmp, FsStyles.CONTEXT_MENU_BOX);
		createContextMenu(boxCmp);

		addShellListener(new ActionsShellListener());
	}

	protected void createContextMenu(Composite boxCmp) {
		ActionsSelListener asl = new ActionsSelListener();
		for (String actionId : DEFAULT_ACTIONS) {
			Button btn = new Button(boxCmp, SWT.FLAT | SWT.PUSH | SWT.LEAD);
			btn.setText(getLabel(actionId));
			btn.setLayoutData(EclipseUiUtils.fillWidth());
			CmsSwtUtils.markup(btn);
			CmsSwtUtils.style(btn, actionId + FsStyles.BUTTON_SUFFIX);
			btn.setData(KEY_ACTION_ID, actionId);
			btn.addSelectionListener(asl);
			actionButtons.put(actionId, btn);
		}
	}

	protected String getLabel(String actionId) {
		switch (actionId) {
		case ACTION_ID_CREATE_FOLDER:
			return "Create Folder";
		case ACTION_ID_BOOKMARK_FOLDER:
			return "Bookmark Folder";
		case ACTION_ID_SHARE_FOLDER:
			return "Share Folder";
		case ACTION_ID_DOWNLOAD_FOLDER:
			return "Download as zip archive";
		case ACTION_ID_DELETE:
			return "Delete";
		case ACTION_ID_UPLOAD_FILE:
			return "Upload Files";
		case ACTION_ID_OPEN:
			return "Open";
		default:
			throw new IllegalArgumentException("Unknown action ID " + actionId);
		}
	}

	protected void aboutToShow(Control source, Point location) {
		IStructuredSelection selection = ((IStructuredSelection) browser.getViewer().getSelection());
		boolean emptySel = true;
		boolean multiSel = false;
		boolean isFolder = true;
		if (selection != null && !selection.isEmpty()) {
			emptySel = false;
			multiSel = selection.size() > 1;
			if (!multiSel && selection.getFirstElement() instanceof Path) {
				isFolder = Files.isDirectory((Path) selection.getFirstElement());
			}
		}
		if (emptySel) {
			setVisible(true, ACTION_ID_CREATE_FOLDER, ACTION_ID_UPLOAD_FILE);
			setVisible(false, ACTION_ID_SHARE_FOLDER, ACTION_ID_DOWNLOAD_FOLDER, ACTION_ID_DELETE, ACTION_ID_OPEN,
					// to be implemented
					ACTION_ID_BOOKMARK_FOLDER);
		} else if (multiSel) {
			setVisible(true, ACTION_ID_CREATE_FOLDER, ACTION_ID_UPLOAD_FILE, ACTION_ID_DELETE);
			setVisible(false, ACTION_ID_SHARE_FOLDER, ACTION_ID_DOWNLOAD_FOLDER, ACTION_ID_OPEN,
					// to be implemented
					ACTION_ID_BOOKMARK_FOLDER);
		} else if (isFolder) {
			setVisible(true, ACTION_ID_CREATE_FOLDER, ACTION_ID_UPLOAD_FILE, ACTION_ID_DELETE);
			setVisible(false, ACTION_ID_OPEN,
					// to be implemented
					ACTION_ID_SHARE_FOLDER, ACTION_ID_DOWNLOAD_FOLDER, ACTION_ID_BOOKMARK_FOLDER);
		} else {
			setVisible(true, ACTION_ID_CREATE_FOLDER, ACTION_ID_UPLOAD_FILE, ACTION_ID_OPEN, ACTION_ID_DELETE);
			setVisible(false, ACTION_ID_SHARE_FOLDER, ACTION_ID_DOWNLOAD_FOLDER,
					// to be implemented
					ACTION_ID_BOOKMARK_FOLDER);
		}
	}

	private void setVisible(boolean visible, String... buttonIds) {
		for (String id : buttonIds) {
			Button button = actionButtons.get(id);
			button.setVisible(visible);
			GridData gd = (GridData) button.getLayoutData();
			gd.heightHint = visible ? SWT.DEFAULT : 0;
		}
	}

	public void show(Control source, Point location, Path currFolderPath) {
		if (isVisible())
			setVisible(false);
		// TODO find a better way to retrieve the parent path (cannot be deduced
		// from table content because it will fail on an empty folder)
		this.currFolderPath = currFolderPath;
		aboutToShow(source, location);
		pack();
		layout();
		if (source instanceof Control)
			setLocation(((Control) source).toDisplay(location.x, location.y));
		open();
	}

	class StyleButton extends Label {
		private static final long serialVersionUID = 7731102609123946115L;

		public StyleButton(Composite parent, int swtStyle) {
			super(parent, swtStyle);
		}

	}

	// class ActionsMouseListener extends MouseAdapter {
	// private static final long serialVersionUID = -1041871937815812149L;
	//
	// @Override
	// public void mouseDown(MouseEvent e) {
	// Object eventSource = e.getSource();
	// if (e.button == 1) {
	// if (eventSource instanceof Button) {
	// Button pressedBtn = (Button) eventSource;
	// String actionId = (String) pressedBtn.getData(KEY_ACTION_ID);
	// switch (actionId) {
	// case ACTION_ID_CREATE_FOLDER:
	// createFolder();
	// break;
	// case ACTION_ID_DELETE:
	// deleteItems();
	// break;
	// default:
	// throw new IllegalArgumentException("Unimplemented action " + actionId);
	// // case ACTION_ID_SHARE_FOLDER:
	// // return "Share Folder";
	// // case ACTION_ID_DOWNLOAD_FOLDER:
	// // return "Download as zip archive";
	// // case ACTION_ID_UPLOAD_FILE:
	// // return "Upload Files";
	// // case ACTION_ID_OPEN:
	// // return "Open";
	// }
	// }
	// }
	// viewer.getControl().setFocus();
	// // setVisible(false);
	// }
	// }

	class ActionsSelListener extends SelectionAdapter {
		private static final long serialVersionUID = -1041871937815812149L;

		@Override
		public void widgetSelected(SelectionEvent e) {
			Object eventSource = e.getSource();
			if (eventSource instanceof Button) {
				Button pressedBtn = (Button) eventSource;
				String actionId = (String) pressedBtn.getData(KEY_ACTION_ID);
				switch (actionId) {
				case ACTION_ID_CREATE_FOLDER:
					createFolder();
					break;
				case ACTION_ID_DELETE:
					deleteItems();
					break;
				case ACTION_ID_OPEN:
					openFile();
					break;
				case ACTION_ID_UPLOAD_FILE:
					uploadFiles();
					break;
				default:
					throw new IllegalArgumentException("Unimplemented action " + actionId);
					// case ACTION_ID_SHARE_FOLDER:
					// return "Share Folder";
					// case ACTION_ID_DOWNLOAD_FOLDER:
					// return "Download as zip archive";
					// case ACTION_ID_OPEN:
					// return "Open";
				}
			}
			browser.setFocus();
			// viewer.getControl().setFocus();
			// setVisible(false);

		}
	}

	class ActionsShellListener extends org.eclipse.swt.events.ShellAdapter {
		private static final long serialVersionUID = -5092341449523150827L;

		@Override
		public void shellDeactivated(ShellEvent e) {
			setVisible(false);
		}
	}

	private void openFile() {
		log.warn("Implement single sourced, workbench independant \"Open File\" action");
	}

	private void deleteItems() {
		IStructuredSelection selection = ((IStructuredSelection) browser.getViewer().getSelection());
		if (selection.isEmpty())
			return;

		StringBuilder builder = new StringBuilder();
		@SuppressWarnings("unchecked")
		Iterator<Object> iterator = selection.iterator();
		List<Path> paths = new ArrayList<>();

		while (iterator.hasNext()) {
			Path path = (Path) iterator.next();
			builder.append(path.getFileName() + ", ");
			paths.add(path);
		}
		String msg = "You are about to delete following elements: " + builder.substring(0, builder.length() - 2)
				+ ". Are you sure?";
		if (MessageDialog.openConfirm(this, "Confirm deletion", msg)) {
			for (Path path : paths) {
				try {
					// Might have already been deleted if we are in a tree
					Files.deleteIfExists(path);
				} catch (IOException e) {
					throw new CmsException("Cannot delete path " + path, e);
				}
			}
			browser.refresh();
		}
	}

	private void createFolder() {
		String msg = "Please provide a name.";
		String name = SingleValue.ask("Create folder", msg);
		// TODO enhance check of name validity
		if (EclipseUiUtils.notEmpty(name)) {
			try {
				Path child = currFolderPath.resolve(name);
				if (Files.exists(child))
					throw new CmsException("An item with name " + name + " already exists at "
							+ currFolderPath.toString() + ", cannot create");
				else
					Files.createDirectories(child);
				browser.refresh();
			} catch (IOException e) {
				throw new CmsException("Cannot create folder " + name + " at " + currFolderPath.toString(), e);
			}
		}
	}

	private void uploadFiles() {
		try {
			FileDialog dialog = new FileDialog(browser.getShell(), SWT.MULTI);
			dialog.setText("Choose one or more files to upload");

			if (EclipseUiUtils.notEmpty(dialog.open())) {
				String[] names = dialog.getFileNames();
				// Workaround small differences between RAP and RCP
				// 1. returned names are absolute path on RAP and
				// relative in RCP
				// 2. in RCP we must use getFilterPath that does not
				// exists on RAP
				Method filterMethod = null;
				Path parPath = null;
				try {
					filterMethod = dialog.getClass().getDeclaredMethod("getFilterPath");
					String filterPath = (String) filterMethod.invoke(dialog);
					parPath = Paths.get(filterPath);
				} catch (NoSuchMethodException nsme) { // RAP
				}
				if (names.length == 0)
					return;
				else {
					loop: for (String name : names) {
						Path tmpPath = Paths.get(name);
						if (parPath != null)
							tmpPath = parPath.resolve(tmpPath);
						if (Files.exists(tmpPath)) {
							URI uri = tmpPath.toUri();
							String uriStr = uri.toString();

							if (Files.isDirectory(tmpPath)) {
								MessageDialog.openError(browser.getShell(), "Unimplemented directory import",
										"Upload of directories in the system is not yet implemented");
								continue loop;
							}
							Path targetPath = currFolderPath.resolve(tmpPath.getFileName().toString());
							InputStream in = null;
							try {
								in = new ByteArrayInputStream(Files.readAllBytes(tmpPath));
								Files.copy(in, targetPath);
								Files.delete(tmpPath);
							} finally {
								IOUtils.closeQuietly(in);
							}
							if (log.isDebugEnabled())
								log.debug("copied uploaded file " + uriStr + " to " + targetPath.toString());
						} else {
							String msg = "Cannot copy tmp file from " + tmpPath.toString();
							if (parPath != null)
								msg += "\nPlease remember that file upload fails when choosing files from the \"Recently Used\" bookmarks on some OS";
							MessageDialog.openError(browser.getShell(), "Missing file", msg);
							continue loop;
						}
					}
					browser.refresh();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			MessageDialog.openError(getShell(), "Upload has failed", "Cannot import files to " + currFolderPath);
		}
	}

	public void setCurrFolderPath(Path currFolderPath) {
		this.currFolderPath = currFolderPath;
	}
}
