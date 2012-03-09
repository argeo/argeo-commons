/*
 * Copyright (C) 2007-2012 Mathieu Baudier
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
package org.argeo.security.ldap;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

import org.argeo.ArgeoException;
import org.argeo.security.UserAdminService;
import org.springframework.ldap.core.ContextSource;
import org.springframework.security.Authentication;
import org.springframework.security.GrantedAuthority;
import org.springframework.security.context.SecurityContextHolder;
import org.springframework.security.providers.encoding.PasswordEncoder;
import org.springframework.security.userdetails.UserDetails;
import org.springframework.security.userdetails.ldap.LdapUserDetailsManager;

/** Extends {@link LdapUserDetailsManager} by adding password encoding support. */
public class ArgeoLdapUserDetailsManager extends LdapUserDetailsManager
		implements UserAdminService {
	private String superUsername = "root";
	private ArgeoUserAdminDaoLdap userAdminDao;
	private PasswordEncoder passwordEncoder;
	private final Random random;

	public ArgeoLdapUserDetailsManager(ContextSource contextSource) {
		super(contextSource);
		this.random = createRandom();
	}

	private static Random createRandom() {
		try {
			return SecureRandom.getInstance("SHA1PRNG");
		} catch (NoSuchAlgorithmException e) {
			return new Random(System.currentTimeMillis());
		}
	}

	@Override
	public void changePassword(String oldPassword, String newPassword) {
		Authentication authentication = SecurityContextHolder.getContext()
				.getAuthentication();
		if (authentication == null)
			throw new ArgeoException(
					"Cannot change password without authentication");
		String username = authentication.getName();
		UserDetails userDetails = loadUserByUsername(username);
		String currentPassword = userDetails.getPassword();
		if (currentPassword == null)
			throw new ArgeoException("Cannot access current password");
		if (!passwordEncoder
				.isPasswordValid(currentPassword, oldPassword, null))
			throw new ArgeoException("Old password invalid");
		// Spring Security LDAP 2.0 is buggy when used with OpenLDAP and called
		// with oldPassword argument
		super.changePassword(null, encodePassword(newPassword));
	}

	public void newRole(String role) {
		userAdminDao.createRole(role, superUsername);
	}

	public void synchronize() {
		for (String username : userAdminDao.listUsers())
			loadUserByUsername(username);
		// TODO: find a way to remove from JCR
	}

	public void deleteRole(String role) {
		userAdminDao.deleteRole(role);
	}

	public Set<String> listUsers() {
		return userAdminDao.listUsers();
	}

	public Set<String> listUsersInRole(String role) {
		Set<String> lst = new TreeSet<String>(
				userAdminDao.listUsersInRole(role));
		Iterator<String> it = lst.iterator();
		while (it.hasNext()) {
			if (it.next().equals(superUsername)) {
				it.remove();
				break;
			}
		}
		return lst;
	}

	public List<String> listUserRoles(String username) {
		UserDetails userDetails = loadUserByUsername(username);
		List<String> roles = new ArrayList<String>();
		for (GrantedAuthority ga : userDetails.getAuthorities()) {
			roles.add(ga.getAuthority());
		}
		return Collections.unmodifiableList(roles);
	}

	public Set<String> listEditableRoles() {
		return userAdminDao.listEditableRoles();
	}

	protected String encodePassword(String password) {
		if (!password.startsWith("{")) {
			byte[] salt = new byte[16];
			random.nextBytes(salt);
			return passwordEncoder.encodePassword(password, salt);
		} else {
			return password;
		}
	}

	public void setPasswordEncoder(PasswordEncoder passwordEncoder) {
		this.passwordEncoder = passwordEncoder;
	}

	public void setSuperUsername(String superUsername) {
		this.superUsername = superUsername;
	}

	public void setUserAdminDao(ArgeoUserAdminDaoLdap userAdminDao) {
		this.userAdminDao = userAdminDao;
	}

}
