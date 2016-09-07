package org.argeo.node;

import java.security.Principal;

/** Allows to modify any data. */
public final class DataAdminPrincipal implements Principal {
	// FIXME put auth constants in API
	private final String name = "OU=node";

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

}
