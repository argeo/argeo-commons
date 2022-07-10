package org.argeo.minidesktop;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

public class MiniTextEditor {
	private URL url;
	private Text text;

	public MiniTextEditor(Composite parent, int style) {
		parent.setLayout(new GridLayout());

		Composite toolBar = new Composite(parent, SWT.NONE);
		toolBar.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		toolBar.setLayout(new RowLayout());
		Button load = new Button(toolBar, SWT.FLAT);
		load.setText("\u2191");// up arrow
		load.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				FileDialog fileDialog = new FileDialog(text.getShell());
				String path = fileDialog.open();
				if (path != null) {
					setUrl(path);
				}
			}

		});

		Button save = new Button(toolBar, SWT.FLAT);
		save.setText("\u2193");// down arrow
		// save.setText("\u1F609");// emoji
		save.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				save(url);
			}

		});

		text = new Text(parent, SWT.WRAP | SWT.MULTI | SWT.BORDER | SWT.V_SCROLL);
		text.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
	}

	protected void load(URL url) {
		text.setText("");
		// TODO deal with encoding and binary data
		try (BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream(), StandardCharsets.UTF_8))) {
			String line = null;
			while ((line = in.readLine()) != null) {
				text.append(line + "\n");
			}
			text.setEditable(true);
		} catch (IOException e) {
			if (e instanceof FileNotFoundException) {
				Path path = url2path(url);
				try {
					Files.createFile(path);
					load(url);
					return;
				} catch (IOException e1) {
					e = e1;
				}
			}
			text.setText(e.getMessage());
			text.setEditable(false);
			e.printStackTrace();
			// throw new IllegalStateException("Cannot load " + url, e);
		}
	}

	protected Path url2path(URL url) {
		try {
			Path path = Paths.get(url.toURI());
			return path;
		} catch (URISyntaxException e) {
			throw new IllegalStateException("Cannot convert " + url + " to uri", e);
		}
	}

	protected void save(URL url) {
		if (!url.getProtocol().equals("file"))
			throw new IllegalArgumentException(url.getProtocol() + " protocol is not supported for write");
		Path path = url2path(url);
		try (BufferedWriter out = new BufferedWriter(new OutputStreamWriter(Files.newOutputStream(path)))) {
			out.write(text.getText());
		} catch (IOException e) {
			throw new IllegalStateException("Cannot save " + url + " to " + path, e);
		}
	}

	public void setUrl(URL url) {
		this.url = url;
		if (text != null)
			load(url);
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

		MiniTextEditor miniBrowser = new MiniTextEditor(shell, SWT.NONE);
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
