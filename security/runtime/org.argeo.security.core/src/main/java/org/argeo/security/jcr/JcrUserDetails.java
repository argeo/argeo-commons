package org.argeo.security.jcr;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.argeo.ArgeoException;
import org.springframework.security.GrantedAuthority;
import org.springframework.security.GrantedAuthorityImpl;
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

	public static JcrUserDetails argeoUserToJcrUserDetails(
			JcrArgeoUser argeoUser) {
		try {
			List<GrantedAuthority> gas = new ArrayList<GrantedAuthority>();
			for (String role : argeoUser.getRoles())
				gas.add(new GrantedAuthorityImpl(role));
			return new JcrUserDetails(argeoUser.getHome().getPath(),
					argeoUser.getUsername(), argeoUser.getPassword(),
					argeoUser.getEnabled(), true, true, true,
					gas.toArray(new GrantedAuthority[gas.size()]));
		} catch (Exception e) {
			throw new ArgeoException("Cannot convert " + argeoUser
					+ " to JCR user details", e);
		}
	}

	public static JcrArgeoUser jcrUserDetailsToArgeoUser(Session userSession,
			JcrUserDetails jcrUserDetails) {
		if (!userSession.getUserID().equals(jcrUserDetails.getUsername()))
			throw new ArgeoException("User session has user id "
					+ userSession.getUserID() + " while details has username "
					+ jcrUserDetails.getUsername());

		Node userHome;
		try {
			userHome = userSession.getNode(jcrUserDetails.getHomePath());
		} catch (RepositoryException e) {
			throw new ArgeoException("Cannot retrieve user home with path "
					+ jcrUserDetails.getHomePath(), e);
		}
		List<String> roles = new ArrayList<String>();
		for (GrantedAuthority ga : jcrUserDetails.getAuthorities())
			roles.add(ga.getAuthority());
		return new JcrArgeoUser(userHome, jcrUserDetails.getPassword(), roles,
				jcrUserDetails.isEnabled());

	}

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

	public JcrUserDetails cloneWithNewPassword(String password) {
		return new JcrUserDetails(homePath, getUsername(), password,
				isEnabled(), isAccountNonExpired(), isAccountNonExpired(),
				isAccountNonLocked(), getAuthorities());
	}
}
