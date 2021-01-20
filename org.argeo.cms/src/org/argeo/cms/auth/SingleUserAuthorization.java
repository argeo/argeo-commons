package org.argeo.cms.auth;

import org.osgi.service.useradmin.Authorization;

/**
 * {@link Authorization} for a single user.
 * 
 * @see SingleUserLoginModule
 */
public class SingleUserAuthorization implements Authorization {

	@Override
	public String getName() {
		return System.getProperty("user.name");
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
