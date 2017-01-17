package org.argeo.eclipse.ui.fs;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.argeo.eclipse.ui.ColumnDefinition;
import org.argeo.eclipse.ui.EclipseUiUtils;
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
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;

/**
 * Experimental UI upon Java 7 nio files api: SashForm layout with bookmarks on
 * the left hand side and a simple table on the right hand side.
 */
public class SimpleFsBrowser extends Composite {
	private final static Log log = LogFactory.getLog(SimpleFsBrowser.class);
	private static final long serialVersionUID = -40347919096946585L;

	private Path currSelected;
	private FsTableViewer bookmarksViewer;
	private FsTableViewer directoryDisplayViewer;

	public SimpleFsBrowser(Composite parent, int style) {
		super(parent, style);
		createContent(this);
		// parent.layout(true, true);
	}

	public Viewer getViewer() {
		return directoryDisplayViewer;
	}

	private void createContent(Composite parent) {
		parent.setLayout(EclipseUiUtils.noSpaceGridLayout());

		SashForm form = new SashForm(parent, SWT.HORIZONTAL);
		Composite leftCmp = new Composite(form, SWT.NONE);
		populateBookmarks(leftCmp);

		Composite rightCmp = new Composite(form, SWT.BORDER);
		populateDisplay(rightCmp);
		form.setLayoutData(EclipseUiUtils.fillAll());
		form.setWeights(new int[] { 1, 3 });
	}

	public void setInput(Path... paths) {
		bookmarksViewer.setPathsInput(paths);
		bookmarksViewer.getTable().getParent().layout(true, true);
	}

	private void populateBookmarks(final Composite parent) {
		// GridLayout layout = EclipseUiUtils.noSpaceGridLayout();
		// layout.verticalSpacing = 5;
		parent.setLayout(new GridLayout());

		ISelectionChangedListener selList = new MySelectionChangedListener();

		appendTitle(parent, "My bookmarks");
		bookmarksViewer = new FsTableViewer(parent, SWT.MULTI | SWT.NO_SCROLL);
		Table table = bookmarksViewer.configureDefaultSingleColumnTable(500);
		GridData gd = EclipseUiUtils.fillWidth();
		gd.horizontalIndent = 10;
		table.setLayoutData(gd);
		bookmarksViewer.addSelectionChangedListener(selList);

		appendTitle(parent, "Jcr + File");

		FsTableViewer jcrFilesViewers = new FsTableViewer(parent, SWT.MULTI | SWT.NO_SCROLL);
		table = jcrFilesViewers.configureDefaultSingleColumnTable(500);
		gd = EclipseUiUtils.fillWidth();
		gd.horizontalIndent = 10;
		table.setLayoutData(gd);
		jcrFilesViewers.addSelectionChangedListener(selList);

		// FileSystemProvider fsProvider = new JackrabbitMemoryFsProvider();
		// try {
		// Path testPath = fsProvider.getPath(new URI("jcr+memory:/"));
		// jcrFilesViewers.setPathsInput(testPath);
		// } catch (URISyntaxException e) {
		// // TODO Auto-generated catch block
		// e.printStackTrace();
		// }
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

	private class MySelectionChangedListener implements ISelectionChangedListener {
		@Override
		public void selectionChanged(SelectionChangedEvent event) {
			IStructuredSelection selection = (IStructuredSelection) bookmarksViewer.getSelection();
			if (selection.isEmpty())
				return;
			else {
				Path newSelected = (Path) selection.getFirstElement();
				if (newSelected.equals(currSelected))
					return;
				currSelected = newSelected;
				directoryDisplayViewer.setInput(currSelected, "*");
			}
		}
	}

	private void populateDisplay(final Composite parent) {
		parent.setLayout(EclipseUiUtils.noSpaceGridLayout());
		directoryDisplayViewer = new FsTableViewer(parent, SWT.MULTI);
		List<ColumnDefinition> colDefs = new ArrayList<>();
		colDefs.add(new ColumnDefinition(new FileIconNameLabelProvider(), "Name", 200));
		colDefs.add(new ColumnDefinition(new NioFileLabelProvider(FsUiConstants.PROPERTY_SIZE), "Size", 100));
		colDefs.add(new ColumnDefinition(new NioFileLabelProvider(FsUiConstants.PROPERTY_TYPE), "Type", 250));
		colDefs.add(new ColumnDefinition(new NioFileLabelProvider(FsUiConstants.PROPERTY_LAST_MODIFIED),
				"Last modified", 200));
		Table table = directoryDisplayViewer.configureDefaultTable(colDefs);
		table.setLayoutData(EclipseUiUtils.fillAll());

		table.addKeyListener(new KeyListener() {
			private static final long serialVersionUID = -8083424284436715709L;

			@Override
			public void keyReleased(KeyEvent e) {
			}

			@Override
			public void keyPressed(KeyEvent e) {
				log.debug("Key event received: " + e.keyCode);
				IStructuredSelection selection = (IStructuredSelection) directoryDisplayViewer.getSelection();
				Path selected = null;
				if (!selection.isEmpty())
					selected = ((Path) selection.getFirstElement());
				if (e.keyCode == SWT.CR) {
					if (!Files.isDirectory(selected))
						return;
					if (selected != null) {
						currSelected = selected;
						directoryDisplayViewer.setInput(currSelected, "*");
					}
				} else if (e.keyCode == SWT.BS) {
					currSelected = currSelected.getParent();
					directoryDisplayViewer.setInput(currSelected, "*");
					directoryDisplayViewer.getTable().setFocus();
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
					currSelected = selected;
					directoryDisplayViewer.setInput(currSelected, "*");
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
					currSelected = selected;
					directoryDisplayViewer.setInput(currSelected, "*");
				}
			}
		});
	}
}
