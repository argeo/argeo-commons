package org.argeo.security;

public interface ArgeoSecurityService {
	public void newUser(ArgeoUser argeoUser);

	public void updateUserPassword(String username, String password);

	public void newRole(String role);

	public ArgeoSecurityDao getSecurityDao();
}
