package org.argeo.cms.auth;

import org.osgi.service.useradmin.Authorization;

/**
 * {@link Authorization} for a single user.
 * 
 * @see SingleUserLoginModule
 */
public class SingleUserAuthorization implements Authorization {
	private String name;

	public SingleUserAuthorization(String name) {
		this.name = name;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public boolean hasRole(String name) {
		return true;
	}

	@Override
	public String[] getRoles() {
		return new String[] {};
	}

	@Override
	public String toString() {
		return getName();
	}

}
