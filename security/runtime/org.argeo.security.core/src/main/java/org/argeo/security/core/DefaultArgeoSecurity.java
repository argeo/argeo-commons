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

import org.argeo.security.ArgeoUser;
import org.argeo.security.ArgeoSecurity;
import org.argeo.security.nature.SimpleUserNature;

/** Holds deployment specific security information. */
public class DefaultArgeoSecurity implements ArgeoSecurity {
	private String superUsername = "root";

	public void beforeCreate(ArgeoUser user) {
		SimpleUserNature simpleUserNature;
		try {
			simpleUserNature = SimpleUserNature
					.findSimpleUserNature(user, null);
		} catch (Exception e) {
			simpleUserNature = new SimpleUserNature();
			user.getUserNatures().put("simpleUserNature", simpleUserNature);
		}

		if (simpleUserNature.getLastName() == null
				|| simpleUserNature.getLastName().equals(""))
			simpleUserNature.setLastName("empty");// to prevent issue with sn in
													// LDAP

	}

	public String getSuperUsername() {
		return superUsername;
	}

	public void setSuperUsername(String superUsername) {
		this.superUsername = superUsername;
	}

}
