package org.argeo.osgi.useradmin;

import org.osgi.service.useradmin.Role;
import org.osgi.service.useradmin.UserAdmin;

public interface UserAdminWorkingCopy extends UserAdmin {
	public void commit();

	public void rollback();

	public Boolean isEditable(Role role);

	public <T extends Role> T getPublished(T role);
}
