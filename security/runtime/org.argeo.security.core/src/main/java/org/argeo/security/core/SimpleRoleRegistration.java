package org.argeo.security.core;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.argeo.security.UserAdminService;

/**
 * Register one or many roles via a user admin service. Does nothing if the role
 * is already registered.
 */
public class SimpleRoleRegistration implements Runnable {
	private String role;
	private List<String> roles;
	private UserAdminService userAdminService;

	@Override
	public void run() {
		Set<String> existingRoles = userAdminService.listEditableRoles();
		if (role != null && !existingRoles.contains(role))
			userAdminService.newRole(role);
		for (String r : roles) {
			if (!existingRoles.contains(r))
				userAdminService.newRole(r);
		}
	}

	public void register(UserAdminService userAdminService, Map<?, ?> properties) {
		this.userAdminService = userAdminService;
		run();
	}

	public void setRole(String role) {
		this.role = role;
	}

	public void setRoles(List<String> roles) {
		this.roles = roles;
	}

	public void setUserAdminService(UserAdminService userAdminService) {
		this.userAdminService = userAdminService;
	}

}
