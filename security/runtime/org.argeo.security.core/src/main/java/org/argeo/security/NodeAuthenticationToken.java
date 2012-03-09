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
package org.argeo.security;

import org.springframework.security.GrantedAuthority;
import org.springframework.security.providers.UsernamePasswordAuthenticationToken;

/** Credentials required for the authentication to a node. */
public class NodeAuthenticationToken extends
		UsernamePasswordAuthenticationToken {
	private static final long serialVersionUID = 1955222132884795213L;
	private final String url;
	private final String securityWorkspace;

	/** Non authenticated local constructor */
	public NodeAuthenticationToken(Object principal, Object credentials) {
		super(principal, credentials);
		this.url = null;
		this.securityWorkspace = null;
	}

	/** Non authenticated remote constructor */
	public NodeAuthenticationToken(Object principal, Object credentials,
			String url, String workspace) {
		super(principal, credentials);
		this.url = url;
		this.securityWorkspace = workspace;
	}

	/** Authenticated constructor */
	public NodeAuthenticationToken(NodeAuthenticationToken sat,
			GrantedAuthority[] authorities) {
		super(sat.getPrincipal(), sat.getCredentials(), authorities);
		this.url = sat.getUrl();
		this.securityWorkspace = sat.getSecurityWorkspace();
	}

	public String getUrl() {
		return url;
	}

	public String getSecurityWorkspace() {
		return securityWorkspace;
	}

	public Boolean isRemote() {
		return url != null;
	}
}
