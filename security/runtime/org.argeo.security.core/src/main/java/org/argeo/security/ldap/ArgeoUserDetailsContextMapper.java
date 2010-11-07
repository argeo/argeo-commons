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

package org.argeo.security.ldap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.argeo.security.ArgeoUser;
import org.argeo.security.UserNature;
import org.argeo.security.core.ArgeoUserDetails;
import org.springframework.ldap.core.DirContextAdapter;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.security.GrantedAuthority;
import org.springframework.security.userdetails.UserDetails;
import org.springframework.security.userdetails.ldap.UserDetailsContextMapper;

public class ArgeoUserDetailsContextMapper implements UserDetailsContextMapper {
	// private final static Log log = LogFactory
	// .getLog(ArgeoUserDetailsContextMapper.class);

	private List<UserNatureMapper> userNatureMappers = new ArrayList<UserNatureMapper>();

	public UserDetails mapUserFromContext(DirContextOperations ctx,
			String username, GrantedAuthority[] authorities) {
		byte[] arr = (byte[]) ctx.getAttributeSortedStringSet("userPassword")
				.first();
		String password = new String(arr);

		Map<String, UserNature> userNatures = new HashMap<String, UserNature>();
		for (UserNatureMapper userInfoMapper : userNatureMappers) {
			UserNature userNature = userInfoMapper.mapUserInfoFromContext(ctx);
			if (userNature != null)
				userNatures.put(userInfoMapper.getName(), userNature);
		}

		return new ArgeoUserDetails(username,
				Collections.unmodifiableMap(userNatures), password, authorities);
	}

	public void mapUserToContext(UserDetails user, DirContextAdapter ctx) {
		ctx.setAttributeValues("objectClass", new String[] { "inetOrgPerson" });
		ctx.setAttributeValue("uid", user.getUsername());
		ctx.setAttributeValue("userPassword", user.getPassword());
		if (user instanceof ArgeoUser) {
			ArgeoUser argeoUser = (ArgeoUser) user;
			for (UserNature userNature : argeoUser.getUserNatures().values()) {
				for (UserNatureMapper userInfoMapper : userNatureMappers) {
					if (userInfoMapper.supports(userNature)) {
						userInfoMapper.mapUserInfoToContext(userNature, ctx);
						break;// use the first mapper found and no others
					}
				}
			}
		}
	}

	public void setUserNatureMappers(List<UserNatureMapper> userNatureMappers) {
		this.userNatureMappers = userNatureMappers;
	}

}
