/*
 * Copyright (C) 2007-2012 Argeo GmbH
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

import org.springframework.security.provisioning.UserDetailsManager;

/** Enrich {@link UserDetailsManager} in order to provide roles semantics. */
public interface UserAdminService extends UserDetailsManager {
	/**
	 * Usernames must match this regexp pattern ({@value #USERNAME_PATTERN}).
	 * Thanks to <a href=
	 * "http://www.mkyong.com/regular-expressions/how-to-validate-username-with-regular-expression/"
	 * >this tip</a> (modified to add upper-case, add '@')
	 */
	//public final static String USERNAME_PATTERN = "^[a-zA-Z0-9_-@]{3,64}$";

	/**
	 * Email addresses must match this regexp pattern ({@value #EMAIL_PATTERN}.
	 * Thanks to <a href=
	 * "http://www.mkyong.com/regular-expressions/how-to-validate-email-address-with-regular-expression/"
	 * >this tip</a>.
	 */
	public final static String EMAIL_PATTERN = "^[_A-Za-z0-9-]+(\\.[_A-Za-z0-9-]+)*@[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$";

	/*
	 * USERS
	 */
	/** List all users. */
	public Set<String> listUsers();

	/** List users having this role (except the super user). */
	public Set<String> listUsersInRole(String role);

	/** Synchronize with the underlying DAO. */
	public void synchronize();

	/*
	 * ROLES
	 */
	public void newRole(String role);

	public Set<String> listEditableRoles();

	public void deleteRole(String role);
}
