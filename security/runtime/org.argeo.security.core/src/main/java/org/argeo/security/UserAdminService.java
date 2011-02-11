package org.argeo.security;

import java.util.Set;

public interface UserAdminService {
	/*
	 * USERS
	 */
	public void newUser(ArgeoUser argeoUser);

	public ArgeoUser getUser(String username);

	public Set<ArgeoUser> listUsers();

	public Boolean userExists(String username);

	public void updateUser(ArgeoUser user);

	public void updateUserPassword(String username, String password);

	/** List users having this role (except the super user). */
	public Set<ArgeoUser> listUsersInRole(String role);

	public void deleteUser(String username);

	/*
	 * ROLES
	 */
	public void newRole(String role);

	public Set<String> listEditableRoles();

	public void deleteRole(String role);

	/*
	 * SYSTEM
	 */
	public Runnable wrapWithSystemAuthentication(final Runnable runnable);
}
