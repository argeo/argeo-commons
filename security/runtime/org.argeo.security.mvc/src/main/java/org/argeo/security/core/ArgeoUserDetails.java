package org.argeo.security.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.argeo.security.ArgeoUser;
import org.argeo.security.UserNature;
import org.springframework.security.GrantedAuthority;
import org.springframework.security.userdetails.User;

public class ArgeoUserDetails extends User implements ArgeoUser {
	private static final long serialVersionUID = 1L;

	private final List<UserNature> userInfos;
	private final List<String> roles;

	public ArgeoUserDetails(String username, List<UserNature> userInfos,
			String password, GrantedAuthority[] authorities)
			throws IllegalArgumentException {
		super(username, password, true, true, true, true, authorities);
		this.userInfos = Collections.unmodifiableList(userInfos);
		
		// Roles
		List<String> roles = new ArrayList<String>();
		for (GrantedAuthority authority : getAuthorities()) {
			roles.add(authority.getAuthority());
		}
		this.roles = Collections.unmodifiableList(roles);
	}

	public List<UserNature> getUserNatures() {
		return userInfos;
	}

	public List<String> getRoles() {
		return roles;
	}
}
