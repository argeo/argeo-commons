package org.argeo.osgi.useradmin;

import java.util.Arrays;
import java.util.Collections;
import java.util.Dictionary;
import java.util.List;

import org.osgi.service.useradmin.Authorization;
import org.osgi.service.useradmin.Role;
import org.osgi.service.useradmin.User;

class LdifAuthorization implements Authorization {
	private final String name;
	private final String displayName;
	private final List<String> allRoles;

	@SuppressWarnings("unchecked")
	public LdifAuthorization(User user, List<Role> allRoles) {
		if (user == null) {
			this.name = null;
			this.displayName = "anonymous";
		} else {
			this.name = user.getName();
			Dictionary<String, Object> props = user.getProperties();
			Object displayName = props.get(LdifName.displayName);
			if (displayName == null)
				displayName = props.get(LdifName.cn);
			if (displayName == null)
				displayName = props.get(LdifName.uid);
			if (displayName == null)
				displayName = user.getName();
			if (displayName == null)
				throw new UserDirectoryException("Cannot set display name for "
						+ user);
			this.displayName = displayName.toString();
		}
		// roles
		String[] roles = new String[allRoles.size()];
		for (int i = 0; i < allRoles.size(); i++) {
			roles[i] = allRoles.get(i).getName();
		}
		this.allRoles = Collections.unmodifiableList(Arrays.asList(roles));
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public boolean hasRole(String name) {
		return allRoles.contains(name);
	}

	@Override
	public String[] getRoles() {
		return allRoles.toArray(new String[allRoles.size()]);
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
