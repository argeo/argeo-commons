package org.argeo.security.ui.admin.editors;

import javax.jcr.Node;

import org.argeo.ArgeoException;
import org.argeo.jcr.ArgeoNames;
import org.argeo.security.ArgeoUser;
import org.argeo.security.SimpleArgeoUser;
import org.argeo.security.UserAdminService;
import org.argeo.security.jcr.JcrUserDetails;
import org.argeo.security.nature.SimpleUserNature;
import org.argeo.security.ui.admin.SecurityAdminPlugin;
import org.argeo.security.ui.admin.views.UsersView;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.editor.FormEditor;
import org.springframework.security.userdetails.UserDetailsManager;

/** Editor for an Argeo user. */
public class ArgeoUserEditor extends FormEditor {
	public final static String ID = "org.argeo.security.ui.admin.adminArgeoUserEditor";

	private ArgeoUser user;
	private JcrUserDetails userDetails;
	private Node userHome;
	private UserAdminService userAdminService;
	private UserDetailsManager userDetailsManager;

	public void init(IEditorSite site, IEditorInput input)
			throws PartInitException {
		super.init(site, input);
		userHome = ((ArgeoUserEditorInput) getEditorInput()).getUserHome();
		String username = ((ArgeoUserEditorInput) getEditorInput())
				.getUsername();

		userDetails = (JcrUserDetails) userDetailsManager
				.loadUserByUsername(username);

		if (username == null) {// new
			user = new SimpleArgeoUser();
			user.getUserNatures().put(SimpleUserNature.TYPE,
					new SimpleUserNature());
		} else
			user = userAdminService.getUser(username);

		this.setPartProperty("name", username != null ? username : "<new user>");
		setPartName(username != null ? username : "<new user>");
	}

	protected void addPages() {
		try {
			addPage(new DefaultUserMainPage(this,
					userHome.getNode(ArgeoNames.ARGEO_USER_PROFILE)));
			addPage(new UserRolesPage(this, userDetails, userAdminService));
		} catch (Exception e) {
			throw new ArgeoException("Cannot add pages", e);
		}
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
		// list pages
		// TODO: make it more generic
		DefaultUserMainPage defaultUserMainPage = (DefaultUserMainPage) findPage(DefaultUserMainPage.ID);
		if (defaultUserMainPage.isDirty()) {
			defaultUserMainPage.doSave(monitor);
			String newPassword = defaultUserMainPage.getNewPassword();
			defaultUserMainPage.resetNewPassword();
			if (newPassword != null)
				userDetails = userDetails.cloneWithNewPassword(newPassword);
		}

		UserRolesPage userRolesPage = (UserRolesPage) findPage(UserRolesPage.ID);
		if (userRolesPage.isDirty()) {
			userRolesPage.doSave(monitor);
			userDetails = userDetails.cloneWithNewRoles(userRolesPage
					.getRoles());
		}

		userDetailsManager.updateUser(userDetails);

		// if (userAdminService.userExists(user.getUsername()))
		// userAdminService.updateUser(user);
		// else {
		// userAdminService.newUser(user);
		// setPartName(user.getUsername());
		// }
		firePropertyChange(PROP_DIRTY);

		userRolesPage.setUserDetails(userDetails);

		// refresh users view
		IWorkbench iw = SecurityAdminPlugin.getDefault().getWorkbench();
		UsersView usersView = (UsersView) iw.getActiveWorkbenchWindow()
				.getActivePage().findView(UsersView.ID);
		usersView.refresh();
	}

	@Override
	public void doSaveAs() {
	}

	@Override
	public boolean isSaveAsAllowed() {
		return false;
	}

	public void refresh() {
		UserRolesPage userRolesPage = (UserRolesPage) findPage(UserRolesPage.ID);
		userRolesPage.refresh();
	}

	public void setUserAdminService(UserAdminService userAdminService) {
		this.userAdminService = userAdminService;
	}

	public void setUserDetailsManager(UserDetailsManager userDetailsManager) {
		this.userDetailsManager = userDetailsManager;
	}

}
