package org.argeo.security.jackrabbit;

import java.security.Principal;

/** Principal for non-interactive system actions. */
class ArgeoSystemPrincipal implements Principal {
	private String name;

	public ArgeoSystemPrincipal(String name) {
		super();
		this.name = name;
	}

	public String getName() {
		return name;
	}

	@Override
	public int hashCode() {
		return getName().hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof ArgeoSystemPrincipal))
			return false;
		return getName().equals(((ArgeoSystemPrincipal) obj).getName());
	}

	@Override
	public String toString() {
		return "Argeo System (non interactive) name=" + getName();
	}

}
