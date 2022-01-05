package org.argeo.api.cms;

import java.security.Principal;

/** Marker for anonymous users. */
public final class AnonymousPrincipal implements Principal {
	private final String name = CmsConstants.ROLE_ANONYMOUS;

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
