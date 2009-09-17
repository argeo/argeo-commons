package org.argeo.security.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.argeo.security.ArgeoUser;
import org.argeo.security.BasicArgeoUser;
import org.argeo.security.UserNature;
import org.springframework.security.Authentication;
import org.springframework.security.GrantedAuthority;
import org.springframework.security.GrantedAuthorityImpl;
import org.springframework.security.userdetails.User;
import org.springframework.security.userdetails.UserDetails;

public class ArgeoUserDetails extends User implements ArgeoUser {
	private static final long serialVersionUID = 1L;

	private final List<UserNature> userNatures;
	private final List<String> roles;

	public ArgeoUserDetails(String username, List<UserNature> userNatures,
			String password, GrantedAuthority[] authorities)
			throws IllegalArgumentException {
		super(username, password, true, true, true, true, authorities);
		this.userNatures = Collections.unmodifiableList(userNatures);

		// Roles
		this.roles = Collections.unmodifiableList(addAuthoritiesToRoles(
				getAuthorities(), new ArrayList<String>()));
	}

	public ArgeoUserDetails(ArgeoUser argeoUser) {
		// TODO: password
		this(argeoUser.getUsername(), argeoUser.getUserNatures(), null,
				rolesToAuthorities(argeoUser.getRoles()));
	}

	public List<UserNature> getUserNatures() {
		return userNatures;
	}

	public List<String> getRoles() {
		return roles;
	}

	/** The provided list, for chaining using {@link Collections} */
	protected static List<String> addAuthoritiesToRoles(
			GrantedAuthority[] authorities, List<String> roles) {
		for (GrantedAuthority authority : authorities) {
			roles.add(authority.getAuthority());
		}
		return roles;
	}

	protected static GrantedAuthority[] rolesToAuthorities(List<String> roles) {
		GrantedAuthority[] arr = new GrantedAuthority[roles.size()];
		for (int i = 0; i < roles.size(); i++) {
			arr[i] = new GrantedAuthorityImpl(roles.get(i));
		}
		return arr;
	}

	public static BasicArgeoUser createBasicArgeoUser(UserDetails userDetails) {
		BasicArgeoUser argeoUser = new BasicArgeoUser();
		argeoUser.setUsername(userDetails.getUsername());
		addAuthoritiesToRoles(userDetails.getAuthorities(), argeoUser
				.getRoles());
		return argeoUser;
	}

	public static ArgeoUser asArgeoUser(Authentication authentication) {
		if (authentication.getPrincipal() instanceof ArgeoUser) {
			return (ArgeoUser) authentication.getPrincipal();
		} else {
			BasicArgeoUser argeoUser = new BasicArgeoUser();
			argeoUser.setUsername(authentication.getName());
			addAuthoritiesToRoles(authentication.getAuthorities(), argeoUser
					.getRoles());
			return argeoUser;
		}
	}
}
