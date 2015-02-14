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
package org.argeo.security.ui.internal;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Retrieves information about the current user. Not an API, can change without
 * notice.
 */
public class CurrentUser {
	// public final static String getUsername() {
	// Subject subject = getSubject();
	// if (subject == null)
	// return null;
	// Principal principal = subject.getPrincipals().iterator().next();
	// return principal.getName();
	//
	// }

	public final static String getUsername() {
		return getAuthentication().getName();
	}

	public final static Set<String> roles() {
		Set<String> roles = Collections.synchronizedSet(new HashSet<String>());
		Authentication authentication = getAuthentication();
		for (GrantedAuthority ga : authentication.getAuthorities()) {
			roles.add(ga.getAuthority());
		}
		return Collections.unmodifiableSet(roles);
	}

	public final static Authentication getAuthentication() {
		return SecurityContextHolder.getContext().getAuthentication();
	}

	// public final static Authentication getAuthentication() {
	// Set<Authentication> authens = getSubject().getPrincipals(
	// Authentication.class);
	// if (authens != null && !authens.isEmpty()) {
	// Principal principal = authens.iterator().next();
	// Authentication authentication = (Authentication) principal;
	// return authentication;
	// }
	// throw new ArgeoException("No authentication found");
	// }

	// public final static Subject getSubject() {
	// Subject subject = Subject.getSubject(AccessController.getContext());
	// if (subject == null)
	// throw new ArgeoException("Not authenticated.");
	// return subject;
	// }
}
