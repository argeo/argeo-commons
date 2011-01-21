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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.argeo.ArgeoException;

/**
 * Read-write implementation of an Argeo user. Typically initialized with a
 * generic instance (read-only9 in order to modify a user.
 */
public class SimpleArgeoUser implements ArgeoUser, Serializable {
	private static final long serialVersionUID = 1L;

	private String username;
	private String password;
	private Map<String, UserNature> userNatures = new HashMap<String, UserNature>();
	private List<String> roles = new ArrayList<String>();

	public SimpleArgeoUser() {

	}

	public SimpleArgeoUser(ArgeoUser argeoUser) {
		username = argeoUser.getUsername();
		password = argeoUser.getPassword();
		userNatures = new HashMap<String, UserNature>(
				argeoUser.getUserNatures());
		roles = new ArrayList<String>(argeoUser.getRoles());
	}

	public Map<String, UserNature> getUserNatures() {
		return userNatures;
	}

	public void updateUserNatures(Map<String, UserNature> userNaturesData) {
		updateUserNaturesWithCheck(userNatures, userNaturesData);
	}

	public static void updateUserNaturesWithCheck(
			Map<String, UserNature> userNatures,
			Map<String, UserNature> userNaturesData) {
		// checks consistency
		if (userNatures.size() != userNaturesData.size())
			throw new ArgeoException(
					"It is forbidden to add or remove user natures via this method");

		for (String type : userNatures.keySet()) {
			if (!userNaturesData.containsKey(type))
				throw new ArgeoException(
						"Could not find a user nature of type " + type);
		}

		// for (int i = 0; i < userNatures.size(); i++) {
		// String type = userNatures.get(i).getType();
		// boolean found = false;
		// for (int j = 0; j < userNatures.size(); j++) {
		// String newType = userNaturesData.get(j).getType();
		// if (type.equals(newType))
		// found = true;
		// }
		// if (!found)
		// throw new ArgeoException(
		// "Could not find a user nature of type " + type);
		// }

		for (String key : userNatures.keySet()) {
			userNatures.put(key, userNaturesData.get(key));
		}
	}

	@Override
	public String toString() {
		return username;
	}

	public List<String> getRoles() {
		return roles;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public void setUserNatures(Map<String, UserNature> userNatures) {
		this.userNatures = userNatures;
	}

	public void setRoles(List<String> roles) {
		this.roles = roles;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}
}
