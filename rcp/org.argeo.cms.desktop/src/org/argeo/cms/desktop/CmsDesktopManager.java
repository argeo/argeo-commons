package org.argeo.cms.desktop;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;

public class CmsDesktopManager {
	private Display display;

	private Shell rootShell;
	private Shell toolBarShell;

	public void init() {
		display = Display.getCurrent();
		if (display != null)
			throw new IllegalStateException("Already a display " + display);
		display = new Display();

		int toolBarSize = 48;

		if (isFullScreen()) {
			rootShell = new Shell(display, SWT.NO_TRIM);
			// rootShell.setMaximized(true);
			rootShell.setFullScreen(true);
			Rectangle bounds = display.getBounds();

			rootShell.setSize(bounds.width, bounds.height);
//			Point realSize = rootShell.getSize();
//			rootShell.setBounds(bounds);
//			Rectangle realBounds = rootShell.getBounds();
		} else {
			rootShell = new Shell(display, SWT.SHELL_TRIM);
			Rectangle shellArea = rootShell.computeTrim(200, 200, 800, 480);
			rootShell.setSize(shellArea.width, shellArea.height);
		}

		rootShell.setLayout(new GridLayout(2, false));
		Composite toolBarArea = new Composite(rootShell, SWT.NONE);
		toolBarArea.setLayoutData(new GridData(toolBarSize, rootShell.getSize().y));

		ToolBar toolBar;
		if (isFullScreen()) {
			toolBarShell = new Shell(rootShell, SWT.NO_TRIM | SWT.ON_TOP);
			toolBar = new ToolBar(toolBarShell, SWT.VERTICAL | SWT.FLAT | SWT.BORDER);
			createDock(toolBar);
			toolBarShell.pack();
			toolBarArea.setLayoutData(new GridData(toolBar.getSize().x, toolBar.getSize().y));
		} else {
			toolBar = new ToolBar(toolBarArea, SWT.VERTICAL | SWT.FLAT | SWT.BORDER);
			createDock(toolBar);
			toolBarArea.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
		}

		Composite backgroundArea = new Composite(rootShell, SWT.NONE);
		backgroundArea.setLayout(new GridLayout(6, true));
		DesktopLayer desktopLayer = new DesktopLayer();
		desktopLayer.init(backgroundArea);
		rootShell.open();
		// rootShell.layout(true, true);

		if (toolBarShell != null) {
			toolBarShell.setLocation(new Point(0, 0));
			toolBarShell.open();
		}
	}

	protected void createDock(ToolBar toolBar) {

		// toolBar.setLocation(clientArea.x, clientArea.y);

		ToolItem closeI = new ToolItem(toolBar, SWT.PUSH);
		closeI.setImage(display.getSystemImage(SWT.ICON_ERROR));
		closeI.setToolTipText("Close");
		closeI.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				rootShell.dispose();
			}

		});

		ToolItem searchI = new ToolItem(toolBar, SWT.PUSH);
		searchI.setImage(display.getSystemImage(SWT.ICON_QUESTION));
		searchI.setToolTipText("Search");
		searchI.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				// rootShell.dispose();
			}

		});
		// toolBar.setSize(48, toolBar.getSize().y);
		toolBar.pack();
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

	protected boolean isFullScreen() {
		return true;
	}

	public static void main(String[] args) {
		CmsDesktopManager desktopManager = new CmsDesktopManager();
		desktopManager.init();
		// Runtime.getRuntime().addShutdownHook(new Thread(() ->
		// desktopManager.dispose(), "Dispose desktop manager"));
		desktopManager.run();
		desktopManager.dispose();
	}

}
