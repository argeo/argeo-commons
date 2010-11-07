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
		UserNature.updateUserNaturesWithCheck(userNatures, userNaturesData);
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
