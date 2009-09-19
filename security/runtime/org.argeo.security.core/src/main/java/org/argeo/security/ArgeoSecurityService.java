package org.argeo.security;


public interface ArgeoSecurityService {
	public void newUser(ArgeoUser argeoUser);
	public void newRole(String role);
	public ArgeoSecurityDao getSecurityDao();
}
