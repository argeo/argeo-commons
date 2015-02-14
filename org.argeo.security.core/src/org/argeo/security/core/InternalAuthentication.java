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

import java.util.Collections;

import org.argeo.security.SystemAuthentication;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

/** A token base on a system key used to request a system authentication. */
public class InternalAuthentication extends UsernamePasswordAuthenticationToken
		implements SystemAuthentication {
	private static final long serialVersionUID = -6783376375615949315L;
	public final static String SYSTEM_KEY_DEFAULT = "argeo";

	public InternalAuthentication(String key, String systemUsername,
			String systemRole) {
		super(systemUsername, key, Collections
				.singleton(new SimpleGrantedAuthority(systemRole)));
	}

	public InternalAuthentication(String key) {
		this(key, SystemAuthentication.USERNAME_SYSTEM, SystemAuthentication.ROLE_SYSTEM);
	}

}
