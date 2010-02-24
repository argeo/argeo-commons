package org.argeo.security;

public interface ArgeoSecurityService {
	public void newUser(ArgeoUser argeoUser);
	
	public void updateUser(ArgeoUser user);

	public void updateUserPassword(String username, String password);

	public void updateCurrentUserPassword(String oldPassword, String newPassword);

	public void newRole(String role);

	public ArgeoSecurityDao getSecurityDao();
}
