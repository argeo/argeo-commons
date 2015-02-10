package org.argeo.security.core;

import java.security.Principal;

import org.osgi.service.useradmin.Authorization;

/** Wraps an OSGi {@link Authorization} as a JAAS {@link Principal} */
public final class AuthorizationPrincipal implements Principal {
	private Authorization authorization;

	public AuthorizationPrincipal(Authorization authorization) {
		this.authorization = authorization;
	}

	@Override
	public String getName() {
		return authorization.getName();
	}

	public Authorization getAuthorization() {
		return authorization;
	}

}
