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
import java.util.List;

import org.argeo.ArgeoException;

public class UserNature implements Serializable {
	private static final long serialVersionUID = 1L;

	private String type;

	public String getType() {
		if (type != null)
			return type;
		else
			return getClass().getName();
	}

	public void setType(String type) {
		this.type = type;
	}

	public final static void updateUserNaturesWithCheck(
			List<UserNature> userNatures, List<UserNature> userNaturesData) {
		if (userNatures.size() != userNaturesData.size())
			throw new ArgeoException(
					"It is forbidden to add or remove user natures via this method");
		for (int i = 0; i < userNatures.size(); i++) {
			String type = userNatures.get(i).getType();
			boolean found = false;
			for (int j = 0; j < userNatures.size(); j++) {
				String newType = userNaturesData.get(j).getType();
				if (type.equals(newType))
					found = true;
			}
			if (!found)
				throw new ArgeoException(
						"Could not find a user nature of type " + type);
		}

		for (int i = 0; i < userNatures.size(); i++) {
			userNatures.set(i, userNaturesData.get(i));
		}
	}
}
