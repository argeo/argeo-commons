package org.argeo.security.jcr;

import org.springframework.security.GrantedAuthority;
import org.springframework.security.userdetails.User;

public class JcrUserDetails extends User {
	private static final long serialVersionUID = -3594542993773402380L;
	private final String homePath;

	public JcrUserDetails(String homePath, String username, String password,
			boolean enabled, boolean accountNonExpired,
			boolean credentialsNonExpired, boolean accountNonLocked,
			GrantedAuthority[] authorities) throws IllegalArgumentException {
		super(username, password, enabled, accountNonExpired,
				credentialsNonExpired, accountNonLocked, authorities);
		this.homePath = homePath;
	}

	public String getHomePath() {
		return homePath;
	}

}
