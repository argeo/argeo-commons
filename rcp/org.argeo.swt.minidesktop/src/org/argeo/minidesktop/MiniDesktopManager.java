package org.argeo.minidesktop;

import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;

/** A very minimalistic desktop manager based on Java and Eclipse SWT. */
public class MiniDesktopManager {
	private Display display;

	private Shell rootShell;
	private Shell toolBarShell;
	private CTabFolder tabFolder;
	private int maxTabTitleLength = 16;

	private final boolean fullscreen;
	private final boolean stacking;

	private MiniDesktopImages images;

	public MiniDesktopManager(boolean fullscreen, boolean stacking) {
		this.fullscreen = fullscreen;
		this.stacking = stacking;
	}

	public void init() {
		Display.setAppName("Mini SWT Desktop");
		display = Display.getCurrent();
		if (display != null)
			throw new IllegalStateException("Already a display " + display);
		display = new Display();

		if (display.getTouchEnabled()) {
			System.out.println("Touch enabled.");
		}

		images = new MiniDesktopImages(display);

		int toolBarSize = 48;

		if (isFullscreen()) {
			rootShell = new Shell(display, SWT.NO_TRIM);
			rootShell.setFullScreen(true);
			Rectangle bounds = display.getBounds();
			rootShell.setLocation(0, 0);
			rootShell.setSize(bounds.width, bounds.height);
		} else {
			rootShell = new Shell(display, SWT.CLOSE | SWT.RESIZE);
			Rectangle shellArea = rootShell.computeTrim(200, 200, 800, 480);
			rootShell.setSize(shellArea.width, shellArea.height);
			rootShell.setText(Display.getAppName());
			rootShell.setImage(images.terminalIcon);
		}

		rootShell.setLayout(noSpaceGridLayout(new GridLayout(2, false)));
		Composite toolBarArea = new Composite(rootShell, SWT.NONE);
		toolBarArea.setLayoutData(new GridData(toolBarSize, rootShell.getSize().y));

		ToolBar toolBar;
		if (isFullscreen()) {
			toolBarShell = new Shell(rootShell, SWT.NO_TRIM | SWT.ON_TOP);
			toolBar = new ToolBar(toolBarShell, SWT.VERTICAL | SWT.FLAT | SWT.BORDER);
			createDock(toolBar);
			toolBarShell.pack();
			toolBarArea.setLayoutData(new GridData(toolBar.getSize().x, toolBar.getSize().y));
		} else {
			toolBar = new ToolBar(toolBarArea, SWT.VERTICAL | SWT.FLAT | SWT.BORDER);
			createDock(toolBar);
			toolBarArea.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		}

		if (isStacking()) {
			tabFolder = new CTabFolder(rootShell, SWT.MULTI | SWT.BORDER | SWT.BOTTOM);
			tabFolder.setLayout(noSpaceGridLayout(new GridLayout()));
			tabFolder.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

			Color selectionBackground = display.getSystemColor(SWT.COLOR_LIST_SELECTION);
			tabFolder.setSelectionBackground(selectionBackground);

			// background
			Control background = createBackground(tabFolder);
			CTabItem homeTabItem = new CTabItem(tabFolder, SWT.NONE);
			homeTabItem.setText("Home");
			homeTabItem.setImage(images.homeIcon);
			homeTabItem.setControl(background);
			tabFolder.setFocus();
		} else {
			createBackground(rootShell);
		}

		rootShell.open();
		// rootShell.layout(true, true);

		if (toolBarShell != null) {
			int toolBarShellY = (display.getBounds().height - toolBar.getSize().y) / 2;
			toolBarShell.setLocation(0, toolBarShellY);
			toolBarShell.open();
		}

		long jvmUptime = ManagementFactory.getRuntimeMXBean().getUptime();
		System.out.println("SWT Mini Desktop Manager available in " + jvmUptime + " ms.");
	}

	protected void createDock(ToolBar toolBar) {
		// Terminal
		addToolItem(toolBar, images.terminalIcon, "Terminal", () -> {
			String url = System.getProperty("user.home");
			AppContext appContext = createAppParent(images.terminalIcon);
			new MiniTerminal(appContext.getAppParent(), url) {

				@Override
				protected void exitCalled() {
					if (appContext.shell != null)
						appContext.shell.dispose();
					if (appContext.tabItem != null)
						appContext.tabItem.dispose();
				}
			};
			String title;
			try {
				title = System.getProperty("user.name") + "@" + InetAddress.getLocalHost().getHostName();
			} catch (UnknownHostException e) {
				title = System.getProperty("user.name") + "@localhost";
			}
			if (appContext.shell != null)
				appContext.shell.setText(title);
			if (appContext.tabItem != null) {
				appContext.tabItem.setText(tabTitle(title));
				appContext.tabItem.setToolTipText(title);
			}
			openApp(appContext);
		});

		// Web browser
		addToolItem(toolBar, images.browserIcon, "Browser", () -> {
			String url = "https://start.duckduckgo.com/";
			AppContext appContext = createAppParent(images.browserIcon);
			new MiniBrowser(appContext.getAppParent(), url, false, false) {
				@Override
				protected void titleChanged(String title) {
					if (appContext.shell != null)
						appContext.shell.setText(title);
					if (appContext.tabItem != null) {
						appContext.tabItem.setText(tabTitle(title));
						appContext.tabItem.setToolTipText(title);
					}
				}
			};
			openApp(appContext);
		});

		// File explorer
		addToolItem(toolBar, images.explorerIcon, "Explorer", () -> {
			String url = System.getProperty("user.home");
			AppContext appContext = createAppParent(images.explorerIcon);
			new MiniExplorer(appContext.getAppParent(), url) {

				@Override
				protected void pathChanged(Path path) {
					if (appContext.shell != null)
						appContext.shell.setText(path.toString());
					if (appContext.tabItem != null) {
						appContext.tabItem.setText(path.getFileName().toString());
						appContext.tabItem.setToolTipText(path.toString());
					}
				}
			};
			openApp(appContext);
		});

		// Separator
		new ToolItem(toolBar, SWT.SEPARATOR);

		// Exit
		addToolItem(toolBar, images.exitIcon, "Exit", () -> rootShell.dispose());

		toolBar.pack();
	}

	protected String tabTitle(String title) {
		return title.length() > maxTabTitleLength ? title.substring(0, maxTabTitleLength) : title;
	}

	protected void addToolItem(ToolBar toolBar, Image icon, String name, Runnable action) {
		ToolItem searchI = new ToolItem(toolBar, SWT.PUSH);
		searchI.setImage(icon);
		searchI.setToolTipText(name);
		searchI.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				action.run();
			}

		});
	}

	protected AppContext createAppParent(Image icon) {
		if (isStacking()) {
			Composite appParent = new Composite(tabFolder, SWT.CLOSE);
			appParent.setLayout(noSpaceGridLayout(new GridLayout()));
			CTabItem item = new CTabItem(tabFolder, SWT.CLOSE);
			item.setImage(icon);
			item.setControl(appParent);
			return new AppContext(item);
		} else {
			Shell shell = isFullscreen() ? new Shell(rootShell, SWT.SHELL_TRIM)
					: new Shell(rootShell.getDisplay(), SWT.SHELL_TRIM);
			shell.setImage(icon);
			return new AppContext(shell);
		}
	}

	protected void openApp(AppContext appContext) {
		if (appContext.shell != null) {
			Shell shell = (Shell) appContext.shell;
			shell.open();
			shell.setSize(new Point(800, 480));
		}
		if (appContext.tabItem != null) {
			tabFolder.setFocus();
			tabFolder.setSelection(appContext.tabItem);
		}
	}

	protected Control createBackground(Composite parent) {
		Composite backgroundArea = new Composite(parent, SWT.NONE);
		backgroundArea.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		initBackground(backgroundArea);
		return backgroundArea;
	}

	protected void initBackground(Composite backgroundArea) {
		MiniHomePart homePart = new MiniHomePart() {

			@Override
			protected void fillAppsToolBar(ToolBar toolBar) {
				createDock(toolBar);
			}
		};
		homePart.createUiPart(backgroundArea, null);
	}

	public void run() {
		while (!rootShell.isDisposed()) {
			if (!display.readAndDispatch())
				display.sleep();
		}
	}

	public void dispose() {
		if (!rootShell.isDisposed())
			rootShell.dispose();
	}

	protected boolean isFullscreen() {
		return fullscreen;
	}

	protected boolean isStacking() {
		return stacking;
	}

	protected Image getIconForExt(String ext) {
		Program program = Program.findProgram(ext);
		if (program == null)
			return display.getSystemImage(SWT.ICON_INFORMATION);

		ImageData iconData = program.getImageData();
		if (iconData == null) {
			return display.getSystemImage(SWT.ICON_INFORMATION);
		} else {
			return new Image(display, iconData);
		}

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
			System.out.println("Usage: java " + MiniDesktopManager.class.getName().replace('.', '/') + " [OPTION]");
			System.out.println("A minimalistic desktop manager based on Java and Eclipse SWT.");
			System.out.println("  --fullscreen : take control of the whole screen (default is to run in a window)");
			System.out.println("  --stacking   : open apps as tabs (default is to create new windows)");
			System.out.println("  --help       : print this help and exit");
			System.exit(1);
		}
		boolean fullscreen = options.contains("--fullscreen");
		boolean stacking = options.contains("--stacking");

		MiniDesktopManager desktopManager = new MiniDesktopManager(fullscreen, stacking);
		desktopManager.init();
		desktopManager.run();
		desktopManager.dispose();
		System.exit(0);
	}

	class AppContext {
		private Shell shell;
		private CTabItem tabItem;

		public AppContext(Shell shell) {
			this.shell = shell;
		}

		public AppContext(CTabItem tabItem) {
			this.tabItem = tabItem;
		}

		Composite getAppParent() {
			if (shell != null)
				return shell;
			if (tabItem != null)
				return (Composite) tabItem.getControl();
			throw new IllegalStateException();
		}
	}
}
