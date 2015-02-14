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
package org.argeo.cms.internal.auth;

import java.security.Principal;

import javax.security.auth.Subject;

import org.springframework.security.core.GrantedAuthority;

/**
 * A {@link Principal} which is also a {@link GrantedAuthority}, so that the
 * Spring Security can be used to quickly populate a {@link Subject} principals.
 */
public final class GrantedAuthorityPrincipal implements Principal,
		GrantedAuthority {
	private static final long serialVersionUID = 6768044196343543328L;
	private final String authority;

	public GrantedAuthorityPrincipal(String authority) {
		this.authority = authority;
	}

	@Override
	public String getAuthority() {
		return authority;
	}

	@Override
	public String getName() {
		return authority;
	}

	@Override
	public int hashCode() {
		return getName().hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof GrantedAuthorityPrincipal))
			return false;
		return getName().equals(((GrantedAuthorityPrincipal) obj).getName());
	}

	@Override
	public String toString() {
		return "Granted Authority " + getName();
	}

}
