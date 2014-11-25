package org.argeo.security.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.argeo.security.UserAdminService;

/**
 * Register one or many roles via a user admin service. Does nothing if the role
 * is already registered.
 */
public class SimpleRoleRegistration implements Runnable {
	private final static Log log = LogFactory
			.getLog(SimpleRoleRegistration.class);

	private String role;
	private List<String> roles = new ArrayList<String>();
	private UserAdminService userAdminService;

	@Override
	public void run() {
		Set<String> existingRoles = userAdminService.listEditableRoles();
		if (role != null && !existingRoles.contains(role))
			newRole(role);
		for (String r : roles) {
			if (!existingRoles.contains(r))
				newRole(r);
		}
	}

	protected void newRole(String r) {
		userAdminService.newRole(r);
		log.info("Added role " + r + " required by application.");
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
