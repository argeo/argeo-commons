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

package org.argeo.security.nature;

import org.argeo.ArgeoException;
import org.argeo.security.AbstractUserNature;
import org.argeo.security.ArgeoUser;
import org.argeo.security.UserNature;

@Deprecated
public class SimpleUserNature extends AbstractUserNature {
	/**
	 * No PAI, for internal use within the Argeo Security framework. Will
	 * probably be removed.
	 */
	public final static String TYPE = "simpleUser";

	private static final long serialVersionUID = 1L;
	private String email;
	private String firstName;
	private String lastName;
	private String description;

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	/*
	 * SECURITY UTILITIES
	 */
	/**
	 * Finds a user nature extending {@link SimpleUserNature} in the provided
	 * user.
	 * 
	 * @param user
	 *            the user to scan
	 * @param simpleNatureType
	 *            the type under which a {@link SimpleUserNature} is registered,
	 *            useful if there are many. can be null.
	 * @return the {@link SimpleUserNature}
	 * @throws ArgeoException
	 *             if no simple user nature was found
	 */
	public final static SimpleUserNature findSimpleUserNature(ArgeoUser user,
			String simpleNatureType) {
		SimpleUserNature simpleNature = null;
		if (simpleNatureType != null)
			simpleNature = (SimpleUserNature) user.getUserNatures().get(
					simpleNatureType);
		else
			for (UserNature userNature : user.getUserNatures().values())
				if (userNature instanceof SimpleUserNature)
					simpleNature = (SimpleUserNature) userNature;

		if (simpleNature == null)
			throw new ArgeoException("No simple user nature in user " + user);
		return simpleNature;
	}

}
