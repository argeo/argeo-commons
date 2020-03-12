package org.argeo.api.security;

import java.security.Principal;

import javax.naming.ldap.LdapName;

import org.argeo.api.NodeConstants;

/** Marker for anonymous users. */
public final class AnonymousPrincipal implements Principal {
	private final String name = NodeConstants.ROLE_ANONYMOUS;

	@Override
	public String getName() {
		return name;
	}

	@Override
	public int hashCode() {
		return name.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		return this == obj;
	}

	@Override
	public String toString() {
		return name.toString();
	}

	public LdapName getLdapName(){
		return NodeSecurityUtils.ROLE_ANONYMOUS_NAME;
	}
}
