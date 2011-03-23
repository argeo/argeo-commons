package org.argeo.security.ui.admin.commands;

import org.argeo.ArgeoException;
import org.argeo.security.UserAdminService;
import org.argeo.security.ui.admin.editors.ArgeoUserEditor;
import org.argeo.security.ui.admin.views.RolesView;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.handlers.HandlerUtil;

/** Add a new role. */
public class AddRole extends AbstractHandler {
	public final static String COMMAND_ID = "org.argeo.security.ui.admin.addRole";
	private UserAdminService userAdminService;
	private String rolePrefix = "ROLE_";

	public Object execute(ExecutionEvent event) throws ExecutionException {
		RolesView rolesView = (RolesView) HandlerUtil
				.getActiveWorkbenchWindow(event).getActivePage()
				.findView(RolesView.ID);
		String role = rolesView.getNewRole();
		if (role.trim().equals(""))
			return null;
		if (role.equals(rolesView.getAddNewRoleText()))
			return null;
		role = role.trim().toUpperCase();
		if (!role.startsWith(rolePrefix))
			role = rolePrefix + role;
		if (userAdminService.listEditableRoles().contains(role))
			throw new ArgeoException("Role " + role + " already exists");
		userAdminService.newRole(role);
		rolesView.refresh();

		// refresh editors
		IEditorReference[] refs = HandlerUtil.getActiveWorkbenchWindow(event)
				.getActivePage()
				.findEditors(null, ArgeoUserEditor.ID, IWorkbenchPage.MATCH_ID);
		for (IEditorReference ref : refs) {
			ArgeoUserEditor userEditor = (ArgeoUserEditor) ref.getEditor(false);
			if (userEditor != null) {
				userEditor.refresh();
			}
		}
		return null;
	}

	public void setUserAdminService(UserAdminService userAdminService) {
		this.userAdminService = userAdminService;
	}

}
