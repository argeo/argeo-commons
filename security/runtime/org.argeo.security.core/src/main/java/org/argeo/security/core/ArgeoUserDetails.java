/*
 * Copyright (C) 2010 Mathieu Baudier <mbaudier@argeo.org>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.argeo.security.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.argeo.security.ArgeoUser;
import org.argeo.security.SimpleArgeoUser;
import org.argeo.security.UserNature;
import org.springframework.security.Authentication;
import org.springframework.security.GrantedAuthority;
import org.springframework.security.GrantedAuthorityImpl;
import org.springframework.security.context.SecurityContextHolder;
import org.springframework.security.userdetails.User;
import org.springframework.security.userdetails.UserDetails;

@Deprecated
public class ArgeoUserDetails extends User implements ArgeoUser {
	private static final long serialVersionUID = 1L;
	private final static Log log = LogFactory.getLog(ArgeoUserDetails.class);

	private final Map<String, UserNature> userNatures;
	private final List<String> roles;

	public ArgeoUserDetails(String username,
			Map<String, UserNature> userNatures, String password,
			GrantedAuthority[] authorities) throws IllegalArgumentException {
		super(username, password, true, true, true, true, authorities);
		this.userNatures = Collections.unmodifiableMap(userNatures);

		// Roles
		this.roles = Collections.unmodifiableList(addAuthoritiesToRoles(
				getAuthorities(), new ArrayList<String>()));
	}

	public ArgeoUserDetails(ArgeoUser argeoUser) {
		this(argeoUser.getUsername(), argeoUser.getUserNatures(), argeoUser
				.getPassword(), rolesToAuthorities(argeoUser.getRoles()));
	}

	public Map<String, UserNature> getUserNatures() {
		return userNatures;
	}

	public void updateUserNatures(Map<String, UserNature> userNaturesData) {
		SimpleArgeoUser
				.updateUserNaturesWithCheck(userNatures, userNaturesData);
	}

	public List<String> getRoles() {
		return roles;
	}

	/** The provided list, for chaining using {@link Collections} */
	public static List<String> addAuthoritiesToRoles(
			GrantedAuthority[] authorities, List<String> roles) {
		for (GrantedAuthority authority : authorities) {
			roles.add(authority.getAuthority());
		}
		return roles;
	}

	public static GrantedAuthority[] rolesToAuthorities(List<String> roles) {
		GrantedAuthority[] arr = new GrantedAuthority[roles.size()];
		for (int i = 0; i < roles.size(); i++) {
			String role = roles.get(i);
			if (log.isTraceEnabled())
				log.debug("Convert role " + role + " to authority (i=" + i
						+ ")");
			arr[i] = new GrantedAuthorityImpl(role);
		}
		return arr;
	}

	public static SimpleArgeoUser createSimpleArgeoUser(UserDetails userDetails) {
		if (userDetails instanceof ArgeoUser) {
			return new SimpleArgeoUser((ArgeoUser) userDetails);
		} else {
			SimpleArgeoUser argeoUser = new SimpleArgeoUser();
			argeoUser.setUsername(userDetails.getUsername());
			addAuthoritiesToRoles(userDetails.getAuthorities(),
					argeoUser.getRoles());
			return argeoUser;
		}
	}

	/** Creates an argeo user based on spring authentication */
	public static ArgeoUser asArgeoUser(Authentication authentication) {
		if (authentication == null)
			return null;

		if (authentication.getPrincipal() instanceof ArgeoUser) {
			return new SimpleArgeoUser(
					(ArgeoUser) authentication.getPrincipal());
		} else {
			SimpleArgeoUser argeoUser = new SimpleArgeoUser();
			argeoUser.setUsername(authentication.getName());
			addAuthoritiesToRoles(authentication.getAuthorities(),
					argeoUser.getRoles());
			return argeoUser;
		}
	}

	/** The Spring security context as an argeo user */
	public static ArgeoUser securityContextUser() {
		Authentication authentication = SecurityContextHolder.getContext()
				.getAuthentication();
		ArgeoUser argeoUser = ArgeoUserDetails.asArgeoUser(authentication);
		return argeoUser;
	}
}
