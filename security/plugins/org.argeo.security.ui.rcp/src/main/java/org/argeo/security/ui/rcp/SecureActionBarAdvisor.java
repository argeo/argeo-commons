package org.argeo.security.ui.rcp;

import org.eclipse.jface.action.GroupMarker;
import org.eclipse.jface.action.ICoolBarManager;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.swt.SWT;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.actions.ActionFactory.IWorkbenchAction;
import org.eclipse.ui.application.ActionBarAdvisor;
import org.eclipse.ui.application.IActionBarConfigurer;

public class SecureActionBarAdvisor extends ActionBarAdvisor {
	private IWorkbenchAction exitAction;
	private IWorkbenchAction openPerspectiveDialogAction;
	private IWorkbenchAction showViewMenuAction;
	private IWorkbenchAction preferences;
	private IWorkbenchAction saveAction;
	private IWorkbenchAction saveAllAction;
	private IWorkbenchAction closeAllAction;

	// private final Boolean isRcp;

	public SecureActionBarAdvisor(IActionBarConfigurer configurer, Boolean isRcp) {
		super(configurer);
		// this.isRcp = isRcp;
	}

	protected void makeActions(IWorkbenchWindow window) {
		preferences = ActionFactory.PREFERENCES.create(window);
		register(preferences);
		openPerspectiveDialogAction = ActionFactory.OPEN_PERSPECTIVE_DIALOG
				.create(window);
		register(openPerspectiveDialogAction);
		showViewMenuAction = ActionFactory.SHOW_VIEW_MENU.create(window);
		register(showViewMenuAction);

		exitAction = ActionFactory.QUIT.create(window);
		register(exitAction);

		// Save semantiocs
		saveAction = ActionFactory.SAVE.create(window);
		register(saveAction);
		saveAllAction = ActionFactory.SAVE_ALL.create(window);
		register(saveAllAction);
		closeAllAction = ActionFactory.CLOSE_ALL.create(window);
		register(closeAllAction);

	}

	protected void fillMenuBar(IMenuManager menuBar) {
		MenuManager fileMenu = new MenuManager("&File",
				IWorkbenchActionConstants.M_FILE);
		MenuManager editMenu = new MenuManager("&Edit",
				IWorkbenchActionConstants.M_EDIT);
		MenuManager windowMenu = new MenuManager("&Window",
				IWorkbenchActionConstants.M_WINDOW);

		menuBar.add(fileMenu);
		menuBar.add(editMenu);
		menuBar.add(windowMenu);
		// Add a group marker indicating where action set menus will appear.
		menuBar.add(new GroupMarker(IWorkbenchActionConstants.MB_ADDITIONS));

		// File
		fileMenu.add(saveAction);
		fileMenu.add(saveAllAction);
		fileMenu.add(closeAllAction);
		fileMenu.add(new GroupMarker(IWorkbenchActionConstants.MB_ADDITIONS));
		fileMenu.add(new Separator());
		fileMenu.add(exitAction);

		// Edit
		editMenu.add(preferences);

		// Window
		windowMenu.add(openPerspectiveDialogAction);
		windowMenu.add(showViewMenuAction);
	}

	@Override
	protected void fillCoolBar(ICoolBarManager coolBar) {
		IToolBarManager saveToolbar = new ToolBarManager(SWT.FLAT | SWT.RIGHT);
		saveToolbar.add(saveAction);
		saveToolbar.add(saveAllAction);
		coolBar.add(saveToolbar);
	}

}
