package org.argeo.osgi.useradmin;

import java.util.ArrayList;
import java.util.List;

import org.osgi.service.useradmin.Authorization;
import org.osgi.service.useradmin.Role;

public class LdifAuthorization implements Authorization {
	private final LdifUser user;

	public LdifAuthorization(LdifUser user) {
		this.user = user;
	}

	@Override
	public String getName() {
		return user.getName();
	}

	@Override
	public boolean hasRole(String name) {
		for (Role role : getAllRoles()) {
			if (role.getName().equals(name))
				return true;
		}
		return false;
	}

	@Override
	public String[] getRoles() {
		List<Role> allRoles = getAllRoles();
		String[] res = new String[allRoles.size() + 1];
		res[0] = user.getName();
		for (int i = 0; i < allRoles.size(); i++)
			res[i + 1] = allRoles.get(i).getName();
		return res;
	}

	List<Role> getAllRoles() {
		List<Role> allRoles = new ArrayList<Role>();
		collectRoles(user, allRoles);
		return allRoles;
	}

	private void collectRoles(LdifUser user, List<Role> allRoles) {
		for (LdifGroup group : user.directMemberOf) {
			// TODO check for loops
			allRoles.add(group);
			collectRoles(group, allRoles);
		}
	}

}
