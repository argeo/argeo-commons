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
package org.argeo.security.jackrabbit;

import java.security.Principal;

import org.springframework.security.GrantedAuthority;

/** Wraps a {@link GrantedAuthority} as a principal. */
class GrantedAuthorityPrincipal implements Principal {
	private final GrantedAuthority grantedAuthority;

	public GrantedAuthorityPrincipal(GrantedAuthority grantedAuthority) {
		this.grantedAuthority = grantedAuthority;
	}

	public String getName() {
		return grantedAuthority.getAuthority();
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
