package org.argeo.security.jackrabbit;

import java.security.Principal;

import org.springframework.security.GrantedAuthority;

/** Wraps a {@link GrantedAuthority} as a principal. */
class GrantedAuthorityPrincipal implements Principal {
	private final GrantedAuthority grantedAuthority;

	public GrantedAuthorityPrincipal(GrantedAuthority grantedAuthority) {
		super();
		this.grantedAuthority = grantedAuthority;
	}

	public String getName() {
		return grantedAuthority.getAuthority();
	}

	@Override
	public int hashCode() {
		return getName().hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof GrantedAuthorityPrincipal))
			return false;
		return getName().equals(((GrantedAuthorityPrincipal) obj).getName());
	}

}
