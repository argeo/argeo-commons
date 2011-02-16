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

package org.argeo.security.ldap.nature;

import org.argeo.security.UserNature;
import org.argeo.security.ldap.UserNatureMapper;
import org.argeo.security.nature.CoworkerNature;
import org.springframework.ldap.core.DirContextAdapter;
import org.springframework.ldap.core.DirContextOperations;

public class CoworkerUserNatureMapper implements UserNatureMapper {

	public String getName() {
		return "coworker";
	}

	public UserNature mapUserInfoFromContext(DirContextOperations ctx) {
		CoworkerNature nature = new CoworkerNature();
		nature.setMobile(ctx.getStringAttribute("mobile"));
		nature.setTelephoneNumber(ctx.getStringAttribute("telephoneNumber"));

		if (nature.getMobile() == null && nature.getTelephoneNumber() == null)
			return null;
		else
			return nature;
	}

	public void mapUserInfoToContext(UserNature userInfoArg,
			DirContextAdapter ctx) {
		CoworkerNature nature = (CoworkerNature) userInfoArg;
		if (nature.getMobile() == null || !nature.getMobile().equals("")) {
			ctx.setAttributeValue("mobile", nature.getMobile());
		}
		if (nature.getTelephoneNumber() == null
				|| !nature.getTelephoneNumber().equals("")) {
			ctx.setAttributeValue("telephoneNumber",
					nature.getTelephoneNumber());
		}
	}

	public Boolean supports(UserNature userNature) {
		return userNature instanceof CoworkerNature;
	}

}
