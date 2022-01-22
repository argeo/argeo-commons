package org.argeo.cms.swt.gcr;

import java.nio.file.Path;
import java.nio.file.Paths;

import javax.xml.namespace.QName;

import org.argeo.api.acr.Content;
import org.argeo.cms.acr.fs.FsContentProvider;
import org.argeo.cms.swt.CmsSwtUtils;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;

public class GcrContentTreeView extends Composite {
	private Tree tree;
	private Table table;
	private Content rootContent;

	private Content selected;

	public GcrContentTreeView(Composite parent, int style, Content content) {
		super(parent, style);
		this.rootContent = content;
		this.selected = rootContent;
		setLayout(new GridLayout(2, false));
		initTree();
		GridData treeGd = CmsSwtUtils.fillHeight();
		treeGd.widthHint = 300;
		tree.setLayoutData(treeGd);
		initTable();

		table.setLayoutData(CmsSwtUtils.fillAll());
	}

	protected void initTree() {
		tree = new Tree(this, 0);
		for (Content c : rootContent) {
			TreeItem root = new TreeItem(tree, 0);
			root.setText(c.getName().toString());
			root.setData(c);
			new TreeItem(root, 0);
		}
		tree.addListener(SWT.Expand, event -> {
			final TreeItem root = (TreeItem) event.item;
			TreeItem[] items = root.getItems();
			for (TreeItem item : items) {
				if (item.getData() != null)
					return;
				item.dispose();
			}
			Content content = (Content) root.getData();
			for (Content c : content) {
				TreeItem item = new TreeItem(root, 0);
				item.setText(c.getName().toString());
				item.setData(c);
				boolean hasChildren = true;
				if (hasChildren) {
					new TreeItem(item, 0);
				}
			}
		});
		tree.addListener(SWT.Selection, event -> {
			TreeItem item = (TreeItem) event.item;
			selected = (Content) item.getData();
			refreshTable();
		});
	}

	protected void initTable() {
		table = new Table(this, 0);
		table.setLinesVisible(true);
		table.setHeaderVisible(true);
		TableColumn keyCol = new TableColumn(table, SWT.NONE);
		keyCol.setText("Attribute");
		keyCol.setWidth(200);
		TableColumn valueCol = new TableColumn(table, SWT.NONE);
		valueCol.setText("Value");
		keyCol.setWidth(300);
		refreshTable();
	}

	protected void refreshTable() {
		for (TableItem item : table.getItems()) {
			item.dispose();
		}
		for (QName key : selected.keySet()) {
			TableItem item = new TableItem(table, 0);
			item.setText(0, key.toString());
			Object value = selected.get(key);
			item.setText(1, value.toString());
		}
		table.getColumn(0).pack();
		table.getColumn(1).pack();
	}

	public static void main(String[] args) {
		Path basePath;
		if (args.length > 0) {
			basePath = Paths.get(args[0]);
		} else {
			basePath = Paths.get(System.getProperty("user.home"));
		}

		final Display display = new Display();
		final Shell shell = new Shell(display);
		shell.setText(basePath.toString());
		shell.setLayout(new FillLayout());

		FsContentProvider contentSession = new FsContentProvider(basePath);
//		GcrContentTreeView treeView = new GcrContentTreeView(shell, 0, contentSession.get("/"));

		shell.setSize(shell.computeSize(800, 600));
		shell.open();
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch())
				display.sleep();
		}
		display.dispose();
	}
}
