package org.argeo.cms.osgi.useradmin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.security.auth.x500.X500Principal;

import org.argeo.api.cms.directory.CmsAuthorization;
import org.osgi.service.useradmin.Authorization;

/** An {@link Authorization} which combines roles form various auth sources. */
class AggregatingAuthorization implements CmsAuthorization, Authorization {
	private final String name;
	private final String displayName;
	private final Set<String> systemRoles;
	private final Set<String> roles;

	public AggregatingAuthorization(String name, String displayName, Set<String> systemRoles, String[] roles) {
		this.name = new X500Principal(name).getName();
		this.displayName = displayName;
		this.systemRoles = Collections.unmodifiableSet(new HashSet<>(systemRoles));
		Set<String> temp = new HashSet<>();
		for (String role : roles) {
			if (!temp.contains(role))
				temp.add(role);
		}
		this.roles = Collections.unmodifiableSet(temp);
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
