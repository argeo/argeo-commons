package org.argeo.minidesktop;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;

public class MiniExplorer {
	private Path path;
	private Text addressT;
	private Table browser;

	private boolean showHidden = false;

	public MiniExplorer(Composite parent, String url) {
		this(parent);
		setUrl(url);
	}

	public MiniExplorer(Composite parent) {
		parent.setLayout(noSpaceGridLayout(new GridLayout()));

		Composite toolBar = new Composite(parent, SWT.NONE);
		toolBar.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		toolBar.setLayout(new FillLayout());
		addressT = new Text(toolBar, SWT.SINGLE);
		addressT.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				setUrl(addressT.getText().trim());
			}
		});
		browser = createTable(parent, this.path);

	}

	public void setPath(Path url) {
		this.path = url;
		if (addressT != null)
			addressT.setText(url.toString());
		if (browser != null) {
			Composite parent = browser.getParent();
			browser.dispose();
			browser = createTable(parent, this.path);
			parent.layout(true, true);
		}
		pathChanged(url);
	}

	protected void pathChanged(Path path) {

	}

	protected Table createTable(Composite parent, Path path) {
		Table table = new Table(parent, SWT.BORDER);
		table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		table.addMouseListener(new MouseAdapter() {

			@Override
			public void mouseDoubleClick(MouseEvent e) {
				Point pt = new Point(e.x, e.y);
				TableItem item = table.getItem(pt);
				Path path = (Path) item.getData();
				if (Files.isDirectory(path)) {
					setPath(path);
				} else {
					//Program.launch(path.toString());
				}
			}
		});

		if (path != null) {
			if (path.getParent() != null) {
				TableItem parentTI = new TableItem(table, SWT.NONE);
				parentTI.setText("..");
				parentTI.setData(path.getParent());
			}

			try {
				// directories
				DirectoryStream<Path> ds = Files.newDirectoryStream(path, p -> Files.isDirectory(p) && isShown(p));
				ds.forEach(p -> {
					TableItem ti = new TableItem(table, SWT.NONE);
					ti.setText(p.getFileName().toString() + "/");
					ti.setData(p);
				});
				// files
				ds = Files.newDirectoryStream(path, p -> !Files.isDirectory(p) && isShown(p));
				ds.forEach(p -> {
					TableItem ti = new TableItem(table, SWT.NONE);
					ti.setText(p.getFileName().toString());
					ti.setData(p);
				});
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}
		return table;
	}

	protected boolean isShown(Path path) {
		if (showHidden)
			return true;
		try {
			return !Files.isHidden(path);
		} catch (IOException e) {
			throw new IllegalArgumentException("Cannot check " + path, e);
		}
	}

	public void setUrl(String url) {
		setPath(Paths.get(url));
	}

	private static GridLayout noSpaceGridLayout(GridLayout layout) {
		layout.horizontalSpacing = 0;
		layout.verticalSpacing = 0;
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		return layout;
	}

	public static void main(String[] args) {
		Display display = Display.getCurrent() == null ? new Display() : Display.getCurrent();
		Shell shell = new Shell(display, SWT.SHELL_TRIM);

		String url = args.length > 0 ? args[0] : System.getProperty("user.home");
		new MiniExplorer(shell, url) {

			@Override
			protected void pathChanged(Path path) {
				shell.setText(path.toString());
			}

		};

		shell.open();
		shell.setSize(new Point(800, 480));
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch())
				display.sleep();
		}
	}

}
