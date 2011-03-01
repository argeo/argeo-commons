package org.argeo.security.jackrabbit.spring;

import java.security.Principal;

import org.springframework.security.GrantedAuthority;

/** Wraps a {@link GrantedAuthority} as a prin,cipal. */
public class GrantedAuthorityPrincipal implements Principal {
	private final GrantedAuthority grantedAuthority;

	public GrantedAuthorityPrincipal(GrantedAuthority grantedAuthority) {
		super();
		this.grantedAuthority = grantedAuthority;
	}

	public String getName() {
		return grantedAuthority.getAuthority();
	}

}
