package org.argeo.osgi.useradmin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.osgi.service.useradmin.Authorization;

class AggregatingAuthorization implements Authorization {
	private final String name;
	private final String displayName;
	private final List<String> systemRoles;
	private final List<String> roles;

	public AggregatingAuthorization(String name, String displayName, Set<String> systemRoles, String[] roles) {
		this.name = name;
		this.displayName = displayName;
		this.systemRoles = Collections.unmodifiableList(new ArrayList<String>(systemRoles));
		List<String> temp = new ArrayList<>();
		for (String role : roles) {
			if (!temp.contains(role))
				temp.add(role);
		}
		this.roles = Collections.unmodifiableList(temp);
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public boolean hasRole(String name) {
		if (systemRoles.contains(name))
			return true;
		if (roles.contains(name))
			return true;
		return false;
	}

	@Override
	public String[] getRoles() {
		int size = systemRoles.size() + roles.size();
		List<String> res = new ArrayList<String>(size);
		res.addAll(systemRoles);
		res.addAll(roles);
		return res.toArray(new String[size]);
	}

	@Override
	public int hashCode() {
		if (name == null)
			return super.hashCode();
		return name.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof Authorization))
			return false;
		Authorization that = (Authorization) obj;
		if (name == null)
			return that.getName() == null;
		return name.equals(that.getName());
	}

	@Override
	public String toString() {
		return displayName;
	}

}
