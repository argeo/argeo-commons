package org.argeo.cms.desktop.mini;

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
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;

public class MiniExplorer {
	private Path url;
	private Text addressT;
	private Table browser;

	private boolean showHidden = false;

	public MiniExplorer(Composite parent, int style) {
		parent.setLayout(new GridLayout());

		Composite toolBar = new Composite(parent, SWT.NONE);
		toolBar.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		toolBar.setLayout(new FillLayout());
		addressT = new Text(toolBar, SWT.SINGLE | SWT.BORDER);
		// addressT.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		addressT.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				setUrl(addressT.getText().trim());
			}
		});
		browser = createTable(parent, this.url);

	}

	public void setUrl(Path url) {
		this.url = url;
		if (addressT != null)
			addressT.setText(url.toString());
		if (browser != null) {
			Composite parent = browser.getParent();
			browser.dispose();
			browser = createTable(parent, this.url);
			parent.layout(true, true);
		}
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
					setUrl(path);
				} else {
					Program.launch(path.toString());
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
				DirectoryStream<Path> ds = Files.newDirectoryStream(url, p -> Files.isDirectory(p) && isShown(p));
				ds.forEach(p -> {
					TableItem ti = new TableItem(table, SWT.NONE);
					ti.setText(p.getFileName().toString() + "/");
					ti.setData(p);
				});
				// files
				ds = Files.newDirectoryStream(url, p -> !Files.isDirectory(p) && isShown(p));
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
		setUrl(Paths.get(url));
	}

	public static void main(String[] args) {
		Display display = Display.getCurrent() == null ? new Display() : Display.getCurrent();
		Shell shell = new Shell(display, SWT.SHELL_TRIM);

		MiniExplorer miniBrowser = new MiniExplorer(shell, SWT.NONE);
		String url = args.length > 0 ? args[0] : System.getProperty("user.home");
		miniBrowser.setUrl(url);

		shell.open();
		shell.setSize(new Point(800, 480));
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch())
				display.sleep();
		}
	}

}
