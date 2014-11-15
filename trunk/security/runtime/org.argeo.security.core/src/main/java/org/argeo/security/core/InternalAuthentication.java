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

import org.argeo.security.SystemAuthentication;
import org.springframework.security.GrantedAuthority;
import org.springframework.security.GrantedAuthorityImpl;
import org.springframework.security.adapters.PrincipalSpringSecurityUserToken;

/** A token base on a system key used to request a system authentication. */
public class InternalAuthentication extends PrincipalSpringSecurityUserToken
		implements SystemAuthentication {
	private static final long serialVersionUID = -6783376375615949315L;
	/** 'admin' for consistency with JCR */
	public final static String DEFAULT_SYSTEM_USERNAME = "admin";
	public final static String DEFAULT_SYSTEM_ROLE = "ROLE_SYSTEM";
	public final static String SYSTEM_KEY_PROPERTY = "argeo.security.systemKey";
	public final static String SYSTEM_KEY_DEFAULT = "argeo";

	public InternalAuthentication(String key, String systemUsername,
			String systemRole) {
		super(
				key,
				systemUsername,
				key,
				new GrantedAuthority[] { new GrantedAuthorityImpl(systemRole) },
				systemUsername);
	}

	public InternalAuthentication(String key) {
		this(key, DEFAULT_SYSTEM_USERNAME, DEFAULT_SYSTEM_ROLE);
	}

}
