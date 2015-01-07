/*
 * Copyright (C) 2007-2012 Argeo GmbH
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
import java.util.Collection;
import java.util.List;

import org.argeo.security.OsAuthenticationToken;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

/**
 * Validates an OS authentication. The id is that it will always be
 * authenticated since we are always runnign within an OS, but the fact that the
 * {@link Authentication} works properly depends on the proper OS login module
 * having been called as well. TODO make it more configurable (base roles, is
 * admin)
 */
public class OsAuthenticationProvider implements AuthenticationProvider {
	final static String osUserRole = "ROLE_OS_USER";
	final static String userRole = "ROLE_USER";
	final static String adminRole = "ROLE_ADMIN";

	final static Boolean isAdmin = true;

	public Authentication authenticate(Authentication authentication)
			throws AuthenticationException {
		return new OsAuthenticationToken(getBaseAuthorities());
	}

	public static Collection<? extends GrantedAuthority> getBaseAuthorities() {
		List<GrantedAuthority> auths = new ArrayList<GrantedAuthority>();
		auths.add(new SimpleGrantedAuthority(osUserRole));
		auths.add(new SimpleGrantedAuthority(userRole));
		if (isAdmin)
			auths.add(new SimpleGrantedAuthority(adminRole));
		return auths;
	}

	@SuppressWarnings("rawtypes")
	public boolean supports(Class authentication) {
		return OsAuthenticationToken.class.isAssignableFrom(authentication);
	}

}
