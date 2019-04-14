package org.argeo.cms.desktop.mini;

import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.LocationEvent;
import org.eclipse.swt.browser.LocationListener;
import org.eclipse.swt.browser.TitleListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

public class MiniBrowser {
	private URL url;
	private Text addressT;
	private Browser browser;

	public MiniBrowser(Composite parent, int style) {
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

		browser = new Browser(parent, SWT.WEBKIT);
		browser.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		browser.addLocationListener(new LocationListener() {

			@Override
			public void changing(LocationEvent event) {
			}

			@Override
			public void changed(LocationEvent event) {
				try {
					MiniBrowser.this.url = new URL(browser.getUrl());
					addressT.setText(url.toString());
				} catch (MalformedURLException e) {
					addressT.setText(e.getMessage());
					throw new IllegalArgumentException("Cannot interpet new URL", e);

				}
			}
		});
	}

	public void setUrl(URL url) {
		this.url = url;
		if (addressT != null)
			addressT.setText(url.toString());
		if (browser != null)
			browser.setUrl(url.toString());
	}

	public void setUrl(String url) {
		try {
			setUrl(new URL(url));
		} catch (MalformedURLException e) {
			// try with http
			try {
				setUrl(new URL("http://"+url));
				return;
			} catch (MalformedURLException e1) {
				// nevermind...
			}
			throw new IllegalArgumentException("Cannot interpret URL " + url, e);
		}
	}

	public void addTitleListener(TitleListener titleListener) {
		browser.addTitleListener(titleListener);
	}

	public static void main(String[] args) {
		Display display = Display.getCurrent() == null ? new Display() : Display.getCurrent();
		Shell shell = new Shell(display, SWT.SHELL_TRIM);

		MiniBrowser miniBrowser = new MiniBrowser(shell, SWT.NONE);
		miniBrowser.addTitleListener(e -> shell.setText(e.title));
		String url = args.length > 0 ? args[0] : "http://www.argeo.org";
		miniBrowser.setUrl(url);

		shell.open();
		shell.setSize(new Point(800, 480));
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch())
				display.sleep();
		}
	}

}
