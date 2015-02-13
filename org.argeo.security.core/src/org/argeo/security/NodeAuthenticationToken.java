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
package org.argeo.security;

import java.util.Collection;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

/** Credentials required for the authentication to a node. */
public class NodeAuthenticationToken extends
		UsernamePasswordAuthenticationToken {
	private static final long serialVersionUID = 1955222132884795213L;
	private final String url;

	/** Non authenticated local constructor */
	public NodeAuthenticationToken(Object principal, Object credentials) {
		super(principal, credentials);
		this.url = null;
	}

	/** Non authenticated remote constructor */
	public NodeAuthenticationToken(Object principal, Object credentials,
			String url) {
		super(principal, credentials);
		this.url = url;
	}

	/** Authenticated constructor */
	public NodeAuthenticationToken(NodeAuthenticationToken sat,
			Collection<? extends GrantedAuthority> authorities) {
		super(sat.getPrincipal(), sat.getCredentials(), authorities);
		this.url = sat.getUrl();
	}

	public String getUrl() {
		return url;
	}

	public Boolean isRemote() {
		return url != null;
	}

	public String toString() {
		String username = getName();
		StringBuilder buf = new StringBuilder("groups=");
		for (GrantedAuthority ga : getAuthorities()) {
			if (!ga.getAuthority().equals(username)) {
				buf.append(ga.getAuthority());
				buf.append(',');
			}
		}
		buf.deleteCharAt(buf.length() - 1);
		return "uid=" + getName() + " " + buf.toString();
	}
}
