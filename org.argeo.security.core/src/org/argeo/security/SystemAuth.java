package org.argeo.security;

import java.security.Principal;

public final class SystemAuth implements Principal {
	private final String name = "init";

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
