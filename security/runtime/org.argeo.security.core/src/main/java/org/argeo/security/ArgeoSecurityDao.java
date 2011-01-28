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

package org.argeo.security;

import java.util.List;

/**
 * Access to the users and roles referential (dependent from the underlying
 * storage, e.g. LDAP).
 */
public interface ArgeoSecurityDao {
	// public ArgeoUser getCurrentUser();

	/** List all users */
	public List<ArgeoUser> listUsers();

	/** List roles that can be modified */
	public List<String> listEditableRoles();

	/**
	 * Creates a new user in the underlying storage. <b>DO NOT CALL DIRECTLY</b>
	 * use {@link ArgeoSecurityService#newUser(ArgeoUser)} instead.
	 */
	public void create(ArgeoUser user);

	public void update(ArgeoUser user);

	public void delete(String username);

	/**
	 * Creates a new role in the underlying storage. <b>DO NOT CALL DIRECTLY</b>
	 * use {@link ArgeoSecurityService#newRole(String)} instead.
	 */
	public void createRole(String role, String superuserName);

	public void deleteRole(String role);

	/** List all users having this role. */
	public List<ArgeoUser> listUsersInRole(String role);

	public Boolean userExists(String username);

	public ArgeoUser getUser(String username);

	public ArgeoUser getUserWithPassword(String username);

	public String getDefaultRole();

	/** Validates a raw password against an encoded one. */
	public Boolean isPasswordValid(String encoded, String raw);

	/** Encodes a raw password. */
	public String encodePassword(String raw);
}
