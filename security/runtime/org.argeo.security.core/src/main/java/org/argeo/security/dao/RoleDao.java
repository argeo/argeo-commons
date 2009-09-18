package org.argeo.security.dao;

import java.util.List;

public interface RoleDao {
	public List<String> listEditableRoles();

	public void create(String role);

	public void delete(String role);


}
