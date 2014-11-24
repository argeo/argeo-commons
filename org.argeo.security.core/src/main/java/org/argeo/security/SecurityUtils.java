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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.security.Authentication;
import org.springframework.security.GrantedAuthority;
import org.springframework.security.context.SecurityContext;
import org.springframework.security.context.SecurityContextHolder;
import org.springframework.security.providers.anonymous.AnonymousAuthenticationToken;

/** Static utilities */
public class SecurityUtils {

	private SecurityUtils() {
	}

	/** Whether the current thread has the admin role */
	public static boolean hasCurrentThreadAuthority(String authority) {
		SecurityContext securityContext = SecurityContextHolder.getContext();
		if (securityContext != null) {
			Authentication authentication = securityContext.getAuthentication();
			if (authentication != null) {
				for (GrantedAuthority ga : authentication.getAuthorities())
					if (ga.getAuthority().equals(authority))
						return true;
			}
		}
		return false;
	}

	/**
	 * @return the authenticated username or null if not authenticated /
	 *         anonymous
	 */
	public static String getCurrentThreadUsername() {
		SecurityContext securityContext = SecurityContextHolder.getContext();
		if (securityContext != null) {
			Authentication authentication = securityContext.getAuthentication();
			if (authentication != null) {
				if (authentication instanceof AnonymousAuthenticationToken) {
					return null;
				}
				return authentication.getName();
			}
		}
		return null;
	}

	/**
	 * Returns the display name of the user details (by calling toString() on
	 * it)
	 */
	public static String getUserDetailsDisplayName() {
		SecurityContext securityContext = SecurityContextHolder.getContext();
		if (securityContext != null) {
			Authentication authentication = securityContext.getAuthentication();
			if (authentication != null) {
				if (authentication instanceof AnonymousAuthenticationToken) {
					return null;
				}
				Object details = authentication.getDetails();
				if (details != null)
					return details.toString();
				return authentication.getName();
			}
		}
		return null;
	}

	/**
	 * Converts an array of Spring Security {@link GrantedAuthority} to a
	 * read-only list of strings, for portability and integration
	 */
	public static List<String> authoritiesToStringList(
			GrantedAuthority[] authorities) {
		List<String> lst = new ArrayList<String>();
		for (GrantedAuthority ga : authorities)
			lst.add(ga.getAuthority());
		return Collections.unmodifiableList(lst);
	}
}
