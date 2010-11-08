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

public interface ArgeoSecurityDao {
//	public ArgeoUser getCurrentUser();

	public List<ArgeoUser> listUsers();

	public List<String> listEditableRoles();

	public void create(ArgeoUser user);

	public void update(ArgeoUser user);

	public void delete(String username);

	public void createRole(String role, String superuserName);

	public void deleteRole(String role);

	public Boolean userExists(String username);

	public ArgeoUser getUser(String username);

	public ArgeoUser getUserWithPassword(String username);
	
	public String getDefaultRole();
}
