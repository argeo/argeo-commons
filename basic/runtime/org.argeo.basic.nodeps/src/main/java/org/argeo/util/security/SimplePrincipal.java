package org.argeo.util.security;

import java.security.Principal;

import org.argeo.ArgeoException;

/** Canonical implementation of a {@link Principal} */
public class SimplePrincipal implements Principal {
	private final String name;

	public SimplePrincipal(String name) {
		if (name == null)
			throw new ArgeoException("Principal name cannot be null");
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
