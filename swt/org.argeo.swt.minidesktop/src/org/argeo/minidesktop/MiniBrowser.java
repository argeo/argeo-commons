package org.argeo.minidesktop;

import java.util.Arrays;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.LocationAdapter;
import org.eclipse.swt.browser.LocationEvent;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

/** A very minimalistic web browser based on {@link Browser}. */
public class MiniBrowser {
	private static Point defaultShellSize = new Point(800, 480);

	private Browser browser;
	private Text addressT;

	private final boolean fullscreen;
	private final boolean appMode;

	public MiniBrowser(Composite composite, String url, boolean fullscreen, boolean appMode) {
		this.fullscreen = fullscreen;
		this.appMode = appMode;
		createUi(composite);
		setUrl(url);
	}

	public Control createUi(Composite parent) {
		parent.setLayout(noSpaceGridLayout(new GridLayout()));
		if (!isAppMode()) {
			Control toolBar = createToolBar(parent);
			toolBar.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		}
		Control body = createBody(parent);
		body.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		return body;
	}

	protected Control createToolBar(Composite parent) {
		Composite toolBar = new Composite(parent, SWT.NONE);
		toolBar.setLayout(new FillLayout());
		addressT = new Text(toolBar, SWT.SINGLE);
		addressT.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				setUrl(addressT.getText().trim());
			}
		});
		return toolBar;
	}

	protected Control createBody(Composite parent) {
		browser = new Browser(parent, SWT.NONE);
		if (isFullScreen())
			browser.addKeyListener(new KeyAdapter() {
				@Override
				public void keyPressed(KeyEvent e) {
					if (e.keyCode == 0x77 && e.stateMask == 0x40000) {// Ctrl+W
						browser.getShell().dispose();
					}
				}
			});
		browser.addLocationListener(new LocationAdapter() {
			@Override
			public void changed(LocationEvent event) {
				if (addressT != null)
					addressT.setText(event.location);
			}
		});

		MiniDesktopSpecific.getMiniDesktopSpecific().addBrowserTitleListener(this, browser);
		MiniDesktopSpecific.getMiniDesktopSpecific().addBrowserOpenWindowListener(this, browser);
		return browser;
	}

	public Browser openNewBrowserWindow() {
		if (isFullScreen()) {
			// TODO manage multiple tabs?
			return browser;
		} else {
			Shell newShell = new Shell(browser.getDisplay(), SWT.SHELL_TRIM);
			MiniBrowser newMiniBrowser = new MiniBrowser(newShell, null, false, isAppMode());
			newShell.setSize(defaultShellSize);
			newShell.open();
			return newMiniBrowser.browser;
		}
	}

	protected boolean isFullScreen() {
		return fullscreen;
	}

	void setUrl(String url) {
		if (browser != null && url != null && !url.equals(browser.getUrl()))
			browser.setUrl(url.toString());
	}

	/** Called when URL changed; to be overridden, does nothing by default. */
	protected void urlChanged(String url) {
	}

	/** Called when title changed; to be overridden, does nothing by default. */
	public void titleChanged(String title) {
	}

	protected Browser getBrowser() {
		return browser;
	}

	protected boolean isAppMode() {
		return appMode;
	}

	private static GridLayout noSpaceGridLayout(GridLayout layout) {
		layout.horizontalSpacing = 0;
		layout.verticalSpacing = 0;
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		return layout;
	}

	public static void main(String[] args) {
		List<String> options = Arrays.asList(args);
		if (options.contains("--help")) {
			System.out.println("Usage: java " + MiniBrowser.class.getName().replace('.', '/') + " [OPTION] [URL]");
			System.out.println("A minimalistic web browser Eclipse SWT Browser integration.");
			System.out.println("  --fullscreen : take control of the whole screen (default is to run in a window)");
			System.out.println("  --app        : open without an address bar and a toolbar");
			System.out.println("  --help       : print this help and exit");
			System.exit(1);
		}
		boolean fullscreen = options.contains("--fullscreen");
		boolean appMode = options.contains("--app");
		String url = "https://start.duckduckgo.com/";
		if (options.size() > 0) {
			String last = options.get(options.size() - 1);
			if (!last.startsWith("--"))
				url = last.trim();
		}

		Display display = Display.getCurrent() == null ? new Display() : Display.getCurrent();
		Shell shell;
		if (fullscreen) {
			shell = new Shell(display, SWT.NO_TRIM);
			shell.setFullScreen(true);
			Rectangle bounds = display.getBounds();
			shell.setSize(bounds.width, bounds.height);
		} else {
			shell = new Shell(display, SWT.SHELL_TRIM);
			shell.setSize(defaultShellSize);
		}

		new MiniBrowser(shell, url, fullscreen, appMode) {

			@Override
			public void titleChanged(String title) {
				shell.setText(title);
			}
		};
		shell.open();

		while (!shell.isDisposed()) {
			if (!display.readAndDispatch())
				display.sleep();
		}
	}

}
