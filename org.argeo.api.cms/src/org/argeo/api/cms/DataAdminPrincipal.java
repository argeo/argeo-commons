package org.argeo.api.cms;

import java.security.Principal;

/** Allows to modify any data. */
public final class DataAdminPrincipal implements Principal {
	private final String name = CmsConstants.ROLE_DATA_ADMIN;

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
