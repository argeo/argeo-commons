package org.argeo.node.security;

import java.security.Principal;

import javax.naming.ldap.LdapName;

import org.argeo.node.NodeConstants;

/** Marker for logged in users. */
public final class UserPrincipal implements Principal {
	private final String name = NodeConstants.ROLE_USER;

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
		return NodeSecurityUtils.ROLE_USER_NAME;
	}

}
