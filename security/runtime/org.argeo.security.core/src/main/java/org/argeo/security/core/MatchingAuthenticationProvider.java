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

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.springframework.core.io.Resource;
import org.springframework.security.AuthenticationException;
import org.springframework.security.BadCredentialsException;
import org.springframework.security.GrantedAuthority;
import org.springframework.security.GrantedAuthorityImpl;
import org.springframework.security.providers.UsernamePasswordAuthenticationToken;
import org.springframework.security.providers.dao.AbstractUserDetailsAuthenticationProvider;
import org.springframework.security.userdetails.User;
import org.springframework.security.userdetails.UserDetails;

public class MatchingAuthenticationProvider extends
		AbstractUserDetailsAuthenticationProvider {

	private Resource mapping;
	private Properties properties;

	private List<String> defaultRoles = new ArrayList<String>();

	@Override
	protected void doAfterPropertiesSet() throws Exception {
		properties = new Properties();
		InputStream propIn = mapping.getInputStream();
		try {
			properties.load(propIn);
		} finally {
			propIn.close();
		}
	}

	@Override
	protected void additionalAuthenticationChecks(UserDetails userDetails,
			UsernamePasswordAuthenticationToken authentication)
			throws AuthenticationException {
		if (!userDetails.getPassword().equals(authentication.getCredentials()))
			throw new BadCredentialsException(
					"Invalid credentails provided by "
							+ authentication.getName());
	}

	@Override
	protected UserDetails retrieveUser(String username,
			UsernamePasswordAuthenticationToken authentication)
			throws AuthenticationException {
		String value = properties.getProperty(username);
		if (value == null)
			throw new BadCredentialsException("User " + username
					+ " is not registered");
		List<GrantedAuthority> grantedAuthorities = new ArrayList<GrantedAuthority>();
		for (String role : defaultRoles)
			grantedAuthorities.add(new GrantedAuthorityImpl(role));
		return new User(
				username,
				value,
				true,
				true,
				true,
				true,
				grantedAuthorities
						.toArray(new GrantedAuthority[grantedAuthorities.size()]));
	}

	public void setMapping(Resource mapping) {
		this.mapping = mapping;
	}

	public void setDefaultRoles(List<String> defaultRoles) {
		this.defaultRoles = defaultRoles;
	}

}
