/*
 * Copyright (C) 2007-2012 Mathieu Baudier
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

import java.util.ArrayList;
import java.util.List;

import org.argeo.security.OsAuthenticationToken;
import org.springframework.security.Authentication;
import org.springframework.security.AuthenticationException;
import org.springframework.security.GrantedAuthority;
import org.springframework.security.GrantedAuthorityImpl;
import org.springframework.security.providers.AuthenticationProvider;

/**
 * Validates an OS authentication. The id is that it will always be
 * authenticated since we are always runnign within an OS, but the fact that the
 * {@link Authentication} works properly depends on the proper OS login module
 * having been called as well.
 */
public class OsAuthenticationProvider implements AuthenticationProvider {
	private String osUserRole = "ROLE_OS_USER";
	private String userRole = "ROLE_USER";
	private String adminRole = "ROLE_ADMIN";

	private Boolean isAdmin = true;

	public Authentication authenticate(Authentication authentication)
			throws AuthenticationException {
		return new OsAuthenticationToken(getBaseAuthorities());
	}

	protected GrantedAuthority[] getBaseAuthorities() {
		List<GrantedAuthority> auths = new ArrayList<GrantedAuthority>();
		auths.add(new GrantedAuthorityImpl(osUserRole));
		auths.add(new GrantedAuthorityImpl(userRole));
		if (isAdmin)
			auths.add(new GrantedAuthorityImpl(adminRole));
		return auths.toArray(new GrantedAuthority[auths.size()]);
	}

	@SuppressWarnings("rawtypes")
	public boolean supports(Class authentication) {
		return OsAuthenticationToken.class.isAssignableFrom(authentication);
	}

	public void setOsUserRole(String osUserRole) {
		this.osUserRole = osUserRole;
	}

	public void setUserRole(String userRole) {
		this.userRole = userRole;
	}

	public void setAdminRole(String adminRole) {
		this.adminRole = adminRole;
	}

	public void setIsAdmin(Boolean isAdmin) {
		this.isAdmin = isAdmin;
	}

}
