package org.argeo.node.security;

import java.security.Principal;

import org.argeo.node.NodeConstants;

/** Allows to modify any data. */
public final class DataAdminPrincipal implements Principal {
	private final String name = NodeConstants.ROLE_DATA_ADMIN;

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
		return obj instanceof DataAdminPrincipal;
	}

	@Override
	public String toString() {
		return name.toString();
	}

}
