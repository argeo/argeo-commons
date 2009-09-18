package org.argeo.security.dao;

import java.util.List;

import org.argeo.security.ArgeoUser;

public interface UserDao {
	public List<ArgeoUser> listUsers();

	public void create(ArgeoUser user);

	public void update(ArgeoUser user);

	public void delete(String username);

	public void updatePassword(String oldPassword, String newPassword);

	public Boolean userExists(String username);

	public ArgeoUser getUser(String username);

	public void addRoles(String username, List<String> roles);

	public void removeRoles(String username, List<String> roles);

}
