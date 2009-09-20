package org.argeo.security;

import java.util.List;

public interface ArgeoSecurityDao {
	public ArgeoUser getCurrentUser();

	public List<ArgeoUser> listUsers();

	public List<String> listEditableRoles();

	public void create(ArgeoUser user);

	public void update(ArgeoUser user);

	public void delete(String username);

	public void createRole(String role, String superuserName);

	public void deleteRole(String role);

	public void updatePassword(String oldPassword, String newPassword);

	public Boolean userExists(String username);

	public ArgeoUser getUser(String username);

	public ArgeoUser getUserWithPassword(String username);
}
