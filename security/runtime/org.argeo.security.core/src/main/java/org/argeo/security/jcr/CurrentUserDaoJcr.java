package org.argeo.security.jcr;

import org.argeo.security.CurrentUserDao;

public class CurrentUserDaoJcr implements CurrentUserDao {
	private String defaultRole= "ROLE_USER";

	public void updateCurrentUserPassword(String oldPassword, String newPassword) {
		throw new UnsupportedOperationException(
				"Updating passwords is not supported");
	}

	public String getDefaultRole() {
		return defaultRole;
	}

	public void setDefaultRole(String defaultRole) {
		this.defaultRole = defaultRole;
	}

}
