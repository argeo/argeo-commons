package org.argeo.security.ui.internal;

import java.security.AccessController;
import java.security.Principal;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.security.auth.Subject;

import org.argeo.ArgeoException;
import org.springframework.security.Authentication;
import org.springframework.security.GrantedAuthority;

/**
 * Retrieves information about the current user. Not an API, can change without
 * notice.
 */
public class CurrentUser {
	public final static String getUsername() {
		Subject subject = getSubject();
		if (subject == null)
			return null;
		Principal principal = subject.getPrincipals().iterator().next();
		return principal.getName();

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
		Set<Authentication> authens = getSubject().getPrincipals(
				Authentication.class);
		if (authens != null && !authens.isEmpty()) {
			Principal principal = authens.iterator().next();
			Authentication authentication = (Authentication) principal;
			return authentication;
		}
		throw new ArgeoException("No authentication found");
	}

	public final static Subject getSubject() {
		Subject subject = Subject.getSubject(AccessController.getContext());
		if (subject == null)
			throw new ArgeoException("Not authenticated.");
		return subject;
	}
}
