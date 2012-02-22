package org.argeo.security;

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
}
