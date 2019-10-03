package org.argeo.swt.desktop;

import java.util.Observable;
import java.util.function.BiFunction;

import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.LocationAdapter;
import org.eclipse.swt.browser.LocationEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

/** A minimalistic web browser based on {@link Browser}. */
public class MiniBrowser implements BiFunction<Composite, MiniBrowser.Context, Control> {
	@Override
	public Control apply(Composite parent, MiniBrowser.Context context) {
		parent.setLayout(new GridLayout());
		Control toolBar = createToolBar(parent, context);
		toolBar.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		Control body = createBody(parent, context);
		body.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		return body;
	}

	public Control createToolBar(Composite parent, MiniBrowser.Context context) {
		Composite toolBar = new Composite(parent, SWT.NONE);
		toolBar.setLayout(new FillLayout());
		Text addressT = new Text(toolBar, SWT.SINGLE | SWT.BORDER);
		addressT.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				String url = addressT.getText().trim();
				context.setUrl(url);
			}
		});
		context.addObserver((o, v) -> addressT.setText(((Context) o).getUrl().toString()));
		return toolBar;
	}

	public Control createBody(Composite parent, MiniBrowser.Context context) {
		Browser browser = new Browser(parent, SWT.WEBKIT);
		browser.addLocationListener(new LocationAdapter() {
			@Override
			public void changing(LocationEvent event) {
				if (!context.getUrl().equals(event.location))
					context.setUrl(event.location);
			}
		});
		browser.addTitleListener(e -> context.setTitle(e.title));
		context.addObserver((o, v) -> {
			String url = ((Context) o).getUrl();
			if (!url.equals(browser.getUrl()))
				browser.setUrl(url.toString());
		});
		return browser;
	}

	/** The observable context of this web browser. */
	public static class Context extends Observable {
		private String url;
		private String title = "";

		public void setUrl(String url) {
			this.url = url;
			System.out.println(url);
			setChanged();
			notifyObservers(url);
		}

		public String getUrl() {
			return url;
		}

		public String getTitle() {
			return title;
		}

		public void setTitle(String title) {
			this.title = title;
			setChanged();
			notifyObservers(title);
		}

	}

	public static void main(String[] args) {
		Display display = Display.getCurrent() == null ? new Display() : Display.getCurrent();
		Shell shell = new Shell(display, SWT.SHELL_TRIM);

		MiniBrowser miniBrowser = new MiniBrowser();
		MiniBrowser.Context context = new MiniBrowser.Context();
		miniBrowser.apply(shell, context);
		context.addObserver((o, v) -> shell.setText(((Context) o).getTitle()));
		String url = args.length > 0 ? args[0] : "http://www.argeo.org";
		context.setUrl(url);

		shell.open();
		shell.setSize(new Point(800, 480));
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch())
				display.sleep();
		}
	}

}
