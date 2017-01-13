package org.argeo.eclipse.ui.fs;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.argeo.eclipse.ui.ColumnDefinition;
import org.argeo.eclipse.ui.EclipseUiUtils;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Tree;

/** Draft for some UI upon Java 7 nio files api */
public class SimpleFsTreeBrowser extends Composite {
	private final static Log log = LogFactory.getLog(SimpleFsTreeBrowser.class);
	private static final long serialVersionUID = -40347919096946585L;

	private Path currSelected;
	private FsTreeViewer treeViewer;
	private FsTableViewer directoryDisplayViewer;

	public SimpleFsTreeBrowser(Composite parent, int style) {
		super(parent, style);
		createContent(this);
		// parent.layout(true, true);
	}

	private void createContent(Composite parent) {
		parent.setLayout(EclipseUiUtils.noSpaceGridLayout());
		SashForm form = new SashForm(parent, SWT.HORIZONTAL);
		Composite child1 = new Composite(form, SWT.NONE);
		populateTree(child1);
		Composite child2 = new Composite(form, SWT.BORDER);
		populateDisplay(child2);
		form.setLayoutData(EclipseUiUtils.fillAll());
		form.setWeights(new int[] { 1, 3 });
	}

	public void setInput(Path... paths) {
		treeViewer.setPathsInput(paths);
		treeViewer.getControl().getParent().layout(true, true);
	}

	private void populateTree(final Composite parent) {
		// GridLayout layout = EclipseUiUtils.noSpaceGridLayout();
		// layout.verticalSpacing = 5;
		parent.setLayout(new GridLayout());

		ISelectionChangedListener selList = new MySelectionChangedListener();

		treeViewer = new FsTreeViewer(parent, SWT.MULTI);
		Tree tree = treeViewer.configureDefaultSingleColumnTable(500);
		GridData gd = EclipseUiUtils.fillAll();
		// gd.horizontalIndent = 10;
		tree.setLayoutData(gd);
		treeViewer.addSelectionChangedListener(selList);
	}

	private class MySelectionChangedListener implements ISelectionChangedListener {
		@Override
		public void selectionChanged(SelectionChangedEvent event) {
			IStructuredSelection selection = (IStructuredSelection) treeViewer.getSelection();
			if (selection.isEmpty())
				return;
			else {
				Path newSelected = (Path) selection.getFirstElement();
				if (newSelected.equals(currSelected))
					return;
				currSelected = newSelected;
				if (Files.isDirectory(currSelected))
					directoryDisplayViewer.setInput(currSelected, "*");
			}
		}
	}

	private void populateDisplay(final Composite parent) {
		parent.setLayout(EclipseUiUtils.noSpaceGridLayout());
		directoryDisplayViewer = new FsTableViewer(parent, SWT.MULTI);
		List<ColumnDefinition> colDefs = new ArrayList<>();
		colDefs.add(new ColumnDefinition(new FileIconNameLabelProvider(), "Name", 200, 200));
		colDefs.add(new ColumnDefinition(new NioFileLabelProvider(FsUiConstants.PROPERTY_SIZE), "Size", 100, 100));
		colDefs.add(new ColumnDefinition(new NioFileLabelProvider(FsUiConstants.PROPERTY_TYPE), "Type", 300, 300));
		colDefs.add(new ColumnDefinition(new NioFileLabelProvider(FsUiConstants.PROPERTY_LAST_MODIFIED),
				"Last modified", 100, 100));
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
	}
}
