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

import java.util.Set;

/**
 * Access to the users and roles referential (dependent from the underlying
 * storage, e.g. LDAP).
 */
public interface UserAdminDao {
	/** List all users */
	public Set<String> listUsers();

	/** List roles that can be modified */
	public Set<String> listEditableRoles();

	/**
	 * Creates a new role in the underlying storage. <b>DO NOT CALL DIRECTLY</b>
	 * use {@link ArgeoSecurityService#newRole(String)} instead.
	 */
	public void createRole(String role, String superuserName);

	public void deleteRole(String role);

	/** List all users having this role. */
	public Set<String> listUsersInRole(String role);

}
