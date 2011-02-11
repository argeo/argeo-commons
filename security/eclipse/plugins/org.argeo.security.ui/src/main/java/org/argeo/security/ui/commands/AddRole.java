package org.argeo.security.ui.commands;

import org.argeo.ArgeoException;
import org.argeo.security.ArgeoSecurityService;
import org.argeo.security.ui.views.RolesView;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.handlers.HandlerUtil;

/** Add a new role. */
public class AddRole extends AbstractHandler {
	public final static String COMMAND_ID = "org.argeo.security.ui.addRole";
	private ArgeoSecurityService securityService;
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
		if (securityService.listEditableRoles().contains(role))
			throw new ArgeoException("Role " + role + " already exists");
		securityService.newRole(role);
		rolesView.refresh();
		return null;
	}

	public void setSecurityService(ArgeoSecurityService securityService) {
		this.securityService = securityService;
	}

}
