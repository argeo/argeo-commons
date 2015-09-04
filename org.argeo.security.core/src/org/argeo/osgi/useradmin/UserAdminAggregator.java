package org.argeo.osgi.useradmin;

import org.osgi.service.useradmin.UserAdmin;

public interface UserAdminAggregator {
	public void addUserAdmin(String baseDn, UserAdmin userAdmin);

	public void removeUserAdmin(String baseDn);
}
