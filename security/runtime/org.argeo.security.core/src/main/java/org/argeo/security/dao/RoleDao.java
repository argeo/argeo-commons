package org.argeo.security.dao;

import java.util.List;

import org.argeo.security.ArgeoUser;

public interface RoleDao {
	public List<String> listRoles();

	public void create(String role);

	public List<String> listUserRoles(ArgeoUser user);

}
