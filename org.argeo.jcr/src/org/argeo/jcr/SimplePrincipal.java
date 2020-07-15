package org.argeo.jcr;

import java.security.Principal;

/** Canonical implementation of a {@link Principal} */
class SimplePrincipal implements Principal {
	private final String name;

	public SimplePrincipal(String name) {
		if (name == null)
			throw new IllegalArgumentException("Principal name cannot be null");
		this.name = name;
	}

	public String getName() {
		return name;
	}

	@Override
	public int hashCode() {
		return name.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null)
			return false;
		if (obj instanceof Principal)
			return name.equals((((Principal) obj).getName()));
		return name.equals(obj.toString());
	}

	@Override
	protected Object clone() throws CloneNotSupportedException {
		return new SimplePrincipal(name);
	}

	@Override
	public String toString() {
		return name;
	}

}
