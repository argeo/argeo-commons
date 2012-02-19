package org.argeo.security.ui.rap;

import org.argeo.security.ui.commands.OpenHomePerspective;
import org.eclipse.core.commands.Category;
import org.eclipse.core.commands.Command;
import org.eclipse.jface.action.ICoolBarManager;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.swt.SWT;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.application.ActionBarAdvisor;
import org.eclipse.ui.application.IActionBarConfigurer;
import org.eclipse.ui.commands.ICommandService;

/** Eclipse rap specific action bar advisor */
public class RapActionBarAdvisor extends ActionBarAdvisor {
	private final static String ID_BASE = "org.argeo.security.ui.rap";
	// private final static Log log = LogFactory
	// .getLog(SecureActionBarAdvisor.class);

	/** Null means anonymous */
	private String username = null;

	// private IAction logoutAction;
	// private IWorkbenchAction openPerspectiveDialogAction;
	// private IWorkbenchAction showViewMenuAction;
	// private IWorkbenchAction preferences;
	// private IWorkbenchAction saveAction;
	// private IWorkbenchAction saveAllAction;
	// private IWorkbenchAction closeAllAction;

	public RapActionBarAdvisor(IActionBarConfigurer configurer, String username) {
		super(configurer);
		this.username = username;
	}

	protected void makeActions(IWorkbenchWindow window) {
		// preferences = ActionFactory.PREFERENCES.create(window);
		// register(preferences);
		// openPerspectiveDialogAction = ActionFactory.OPEN_PERSPECTIVE_DIALOG
		// .create(window);
		// register(openPerspectiveDialogAction);
		// showViewMenuAction = ActionFactory.SHOW_VIEW_MENU.create(window);
		// register(showViewMenuAction);
		//
		// // logout
		// logoutAction = ActionFactory.QUIT.create(window);
		// // logoutAction = createLogoutAction();
		// register(logoutAction);
		//
		// // Save semantics
		// saveAction = ActionFactory.SAVE.create(window);
		// register(saveAction);
		// saveAllAction = ActionFactory.SAVE_ALL.create(window);
		// register(saveAllAction);
		// closeAllAction = ActionFactory.CLOSE_ALL.create(window);
		// register(closeAllAction);

	}

	protected void fillMenuBar(IMenuManager menuBar) {
		// MenuManager fileMenu = new MenuManager("&File",
		// IWorkbenchActionConstants.M_FILE);
		// MenuManager editMenu = new MenuManager("&Edit",
		// IWorkbenchActionConstants.M_EDIT);
		// MenuManager windowMenu = new MenuManager("&Window",
		// IWorkbenchActionConstants.M_WINDOW);
		//
		// menuBar.add(fileMenu);
		// menuBar.add(editMenu);
		// menuBar.add(windowMenu);
		// // Add a group marker indicating where action set menus will appear.
		// menuBar.add(new GroupMarker(IWorkbenchActionConstants.MB_ADDITIONS));
		//
		// // File
		// fileMenu.add(saveAction);
		// fileMenu.add(saveAllAction);
		// fileMenu.add(closeAllAction);
		// fileMenu.add(new
		// GroupMarker(IWorkbenchActionConstants.MB_ADDITIONS));
		// fileMenu.add(new Separator());
		// fileMenu.add(logoutAction);
		//
		// // Edit
		// editMenu.add(preferences);
		//
		// // Window
		// windowMenu.add(openPerspectiveDialogAction);
		// windowMenu.add(showViewMenuAction);
	}

	@Override
	protected void fillCoolBar(ICoolBarManager coolBar) {
		if (username != null) {
			ICommandService cmdService = (ICommandService) getActionBarConfigurer()
					.getWindowConfigurer().getWorkbenchConfigurer()
					.getWorkbench().getService(ICommandService.class);
			Category userMenus = cmdService.getCategory(ID_BASE + ".userMenus");
			if (!userMenus.isDefined())
				userMenus.define("User Menus", "User related menus");

			Command userMenu = cmdService.getCommand(ID_BASE
					+ ".userMenuCommand");
			if (userMenu.isDefined())
				userMenu.undefine();
			userMenu.define(username, "User menu actions", userMenus);
			userMenu.setHandler(new OpenHomePerspective());

			// userToolbar.add(new UserMenuAction());
			// coolBar.add(userToolbar);
		} else {// anonymous
			IToolBarManager userToolbar = new ToolBarManager(SWT.FLAT
					| SWT.RIGHT);
			// userToolbar.add(logoutAction);
			coolBar.add(userToolbar);
		}
		// IToolBarManager saveToolbar = new ToolBarManager(SWT.FLAT |
		// SWT.RIGHT);
		// saveToolbar.add(saveAction);
		// saveToolbar.add(saveAllAction);
		// coolBar.add(saveToolbar);
	}

	// class UserMenuAction extends Action implements IWorkbenchAction {
	//
	// public UserMenuAction() {
	// super(username, IAction.AS_DROP_DOWN_MENU);
	// // setMenuCreator(new UserMenu());
	// }
	//
	// @Override
	// public String getId() {
	// return "org.argeo.security.ui.rap.userMenu";
	// }
	//
	// @Override
	// public void dispose() {
	// }
	//
	// }

	// class UserMenu implements IMenuCreator {
	// private Menu menu;
	//
	// public Menu getMenu(Control parent) {
	// Menu menu = new Menu(parent);
	// addActionToMenu(menu, logoutAction);
	// return menu;
	// }
	//
	// private void addActionToMenu(Menu menu, IAction action) {
	// ActionContributionItem item = new ActionContributionItem(action);
	// item.fill(menu, -1);
	// }
	//
	// public void dispose() {
	// if (menu != null) {
	// menu.dispose();
	// }
	// }
	//
	// public Menu getMenu(Menu parent) {
	// // Not use
	// return null;
	// }
	//
	// }

	// protected IAction createLogoutAction() {
	// Subject subject = Subject.getSubject(AccessController.getContext());
	// final String username = subject.getPrincipals().iterator().next()
	// .getName();
	//
	// IAction logoutAction = new Action() {
	// public String getId() {
	// return SecureRapActivator.ID + ".logoutAction";
	// }
	//
	// public String getText() {
	// return "Logout " + username;
	// }
	//
	// public void run() {
	// // try {
	// // Subject subject = SecureRapActivator.getLoginContext()
	// // .getSubject();
	// // String subjectStr = subject.toString();
	// // subject.getPrincipals().clear();
	// // SecureRapActivator.getLoginContext().logout();
	// // log.info(subjectStr + " logged out");
	// // } catch (LoginException e) {
	// // log.error("Error when logging out", e);
	// // }
	// // SecureEntryPoint.logout(username);
	// // PlatformUI.getWorkbench().close();
	// // try {
	// // RWT.getRequest().getSession().setMaxInactiveInterval(1);
	// // } catch (Exception e) {
	// // if (log.isTraceEnabled())
	// // log.trace("Error when invalidating session", e);
	// // }
	// }
	//
	// };
	// return logoutAction;
	// }

}
