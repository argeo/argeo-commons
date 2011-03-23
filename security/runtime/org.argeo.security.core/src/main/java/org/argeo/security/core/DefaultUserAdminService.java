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

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.argeo.security.ArgeoUser;
import org.argeo.security.UserAdminDao;
import org.argeo.security.UserAdminService;
import org.argeo.security.nature.SimpleUserNature;

public class DefaultUserAdminService implements UserAdminService {
	private String superUsername = "root";
	private UserAdminDao userAdminDao;

	public void newRole(String role) {
		userAdminDao.createRole(role, getSuperUsername());
	}

	public void updateUserPassword(String username, String password) {
		userAdminDao.updateUserPassword(username, password);
	}

	public void newUser(ArgeoUser user) {
		// pre-process
		SimpleUserNature simpleUserNature;
		try {
			simpleUserNature = SimpleUserNature
					.findSimpleUserNature(user, null);
		} catch (Exception e) {
			simpleUserNature = new SimpleUserNature();
			user.getUserNatures().put("simpleUserNature", simpleUserNature);
		}

		if (simpleUserNature.getLastName() == null
				|| simpleUserNature.getLastName().equals("")) {
			// to prevent issue with sn in LDAP
			simpleUserNature.setLastName("empty");
		}

		userAdminDao.createUser(user);
	}
	
	

	public void synchronize() {
		// TODO Auto-generated method stub
		
	}

	public ArgeoUser getUser(String username) {
		return userAdminDao.getUser(username);
	}

	public Boolean userExists(String username) {
		return userAdminDao.userExists(username);
	}

	public void updateUser(ArgeoUser user) {
		userAdminDao.updateUser(user);
	}

	public void deleteUser(String username) {
		userAdminDao.deleteUser(username);

	}

	public void deleteRole(String role) {
		userAdminDao.deleteRole(role);
	}

	public Set<ArgeoUser> listUsersInRole(String role) {
		Set<ArgeoUser> lst = new HashSet<ArgeoUser>(
				userAdminDao.listUsersInRole(role));
		Iterator<ArgeoUser> it = lst.iterator();
		while (it.hasNext()) {
			if (it.next().getUsername().equals(getSuperUsername())) {
				it.remove();
				break;
			}
		}
		return lst;
	}

	public Set<ArgeoUser> listUsers() {
		return userAdminDao.listUsers();
	}

	public List<String> listUserRoles(String username) {
		return getUser(username).getRoles();
	}

	public Set<String> listEditableRoles() {
		return userAdminDao.listEditableRoles();
	}

	// TODO: expose it via the interface as well?
	public String getSuperUsername() {
		return superUsername;
	}

	public void setUserAdminDao(UserAdminDao userAdminDao) {
		this.userAdminDao = userAdminDao;
	}

}
