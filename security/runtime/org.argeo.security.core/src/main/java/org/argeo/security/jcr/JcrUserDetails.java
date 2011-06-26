package org.argeo.security.jcr;

import java.util.ArrayList;
import java.util.List;

import org.springframework.security.GrantedAuthority;
import org.springframework.security.GrantedAuthorityImpl;
import org.springframework.security.userdetails.User;

/** User details wrapping a home node. */
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

	/** Clone immutable with new roles */
	public JcrUserDetails cloneWithNewRoles(List<String> roles) {
		List<GrantedAuthority> authorities = new ArrayList<GrantedAuthority>();
		for (String role : roles) {
			authorities.add(new GrantedAuthorityImpl(role));
		}
		return new JcrUserDetails(homePath, getUsername(), getPassword(),
				isEnabled(), isAccountNonExpired(), isAccountNonExpired(),
				isAccountNonLocked(),
				authorities.toArray(new GrantedAuthority[authorities.size()]));
	}

	/** Clone immutable with new password */
	public JcrUserDetails cloneWithNewPassword(String password) {
		return new JcrUserDetails(homePath, getUsername(), password,
				isEnabled(), isAccountNonExpired(), isAccountNonExpired(),
				isAccountNonLocked(), getAuthorities());
	}

	public String getHomePath() {
		return homePath;
	}

}
