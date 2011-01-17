package org.argeo.security.ui.rcp;

import org.eclipse.jface.action.GroupMarker;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
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
	private IWorkbenchAction newWindowAction;
	private IWorkbenchAction preferences;
	private IWorkbenchAction helpContentAction;
	// private IWorkbenchAction aboutAction;

	private final Boolean isRcp;

	public SecureActionBarAdvisor(IActionBarConfigurer configurer, Boolean isRcp) {
		super(configurer);
		this.isRcp = isRcp;
	}

	protected void makeActions(IWorkbenchWindow window) {
		preferences = ActionFactory.PREFERENCES.create(window);
		register(preferences);
		openPerspectiveDialogAction = ActionFactory.OPEN_PERSPECTIVE_DIALOG
				.create(window);
		register(openPerspectiveDialogAction);
		showViewMenuAction = ActionFactory.SHOW_VIEW_MENU.create(window);
		register(showViewMenuAction);
		helpContentAction = ActionFactory.HELP_CONTENTS.create(window);
		register(helpContentAction);

		exitAction = ActionFactory.QUIT.create(window);
		register(exitAction);
		if (isRcp) {
			// aboutAction = ActionFactory.ABOUT.create(window);
			// register(aboutAction);
			newWindowAction = ActionFactory.OPEN_NEW_WINDOW.create(window);
			register(newWindowAction);
		}
	}

	protected void fillMenuBar(IMenuManager menuBar) {
		MenuManager fileMenu = new MenuManager("&File",
				IWorkbenchActionConstants.M_FILE);
		MenuManager editMenu = new MenuManager("&Edit",
				IWorkbenchActionConstants.M_EDIT);
		MenuManager windowMenu = new MenuManager("&Window",
				IWorkbenchActionConstants.M_WINDOW);
		MenuManager helpMenu = new MenuManager("&Help",
				IWorkbenchActionConstants.M_HELP);

		menuBar.add(fileMenu);
		menuBar.add(editMenu);
		menuBar.add(windowMenu);
		// Add a group marker indicating where action set menus will appear.
		menuBar.add(new GroupMarker(IWorkbenchActionConstants.MB_ADDITIONS));
		menuBar.add(helpMenu);

		// File
		if (isRcp) {
			fileMenu.add(newWindowAction);
			fileMenu.add(new Separator());
		}
		fileMenu.add(exitAction);

		// Edit
		editMenu.add(preferences);

		// Window
		windowMenu.add(openPerspectiveDialogAction);
		windowMenu.add(showViewMenuAction);

		// Help
		helpMenu.add(helpContentAction);
		// helpMenu.add(aboutAction);
	}

}
