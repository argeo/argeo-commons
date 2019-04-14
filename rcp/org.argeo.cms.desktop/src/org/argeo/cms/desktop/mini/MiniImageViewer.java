package org.argeo.cms.desktop.mini;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;

public class MiniImageViewer implements PaintListener {
	private URL url;
	private Canvas area;

	private Image image;

	public MiniImageViewer(Composite parent, int style) {
		parent.setLayout(new GridLayout());

		Composite toolBar = new Composite(parent, SWT.NONE);
		toolBar.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		toolBar.setLayout(new RowLayout());
		Button load = new Button(toolBar, SWT.FLAT);
		load.setText("\u2191");// up arrow
		load.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				FileDialog fileDialog = new FileDialog(area.getShell());
				String path = fileDialog.open();
				if (path != null) {
					setUrl(path);
				}
			}

		});

		area = new Canvas(parent, SWT.NONE);
		area.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		area.addPaintListener(this);
	}

	protected void load(URL url) {
		try {
			ImageLoader imageLoader = new ImageLoader();
			ImageData[] data = imageLoader.load(url.openStream());
			image = new Image(area.getDisplay(), data[0]);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void paintControl(PaintEvent e) {
		e.gc.drawImage(image, 0, 0);

	}

	protected Path url2path(URL url) {
		try {
			Path path = Paths.get(url.toURI());
			return path;
		} catch (URISyntaxException e) {
			throw new IllegalStateException("Cannot convert " + url + " to uri", e);
		}
	}

	public void setUrl(URL url) {
		this.url = url;
		if (area != null)
			load(this.url);
	}

	public void setUrl(String url) {
		try {
			setUrl(new URL(url));
		} catch (MalformedURLException e) {
			// try with http
			try {
				setUrl(new URL("file://" + url));
				return;
			} catch (MalformedURLException e1) {
				// nevermind...
			}
			throw new IllegalArgumentException("Cannot interpret URL " + url, e);
		}
	}

	public static void main(String[] args) {
		Display display = Display.getCurrent() == null ? new Display() : Display.getCurrent();
		Shell shell = new Shell(display, SWT.SHELL_TRIM);

		MiniImageViewer miniBrowser = new MiniImageViewer(shell, SWT.NONE);
		String url = args.length > 0 ? args[0] : "";
		if (!url.trim().equals("")) {
			miniBrowser.setUrl(url);
			shell.setText(url);
		} else {
			shell.setText("*");
		}

		shell.open();
		shell.setSize(new Point(800, 480));
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch())
				display.sleep();
		}
	}

}
