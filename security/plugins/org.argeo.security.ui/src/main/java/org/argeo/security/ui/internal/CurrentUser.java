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

public class CurrentUser {
	public final static String getUsername() {
		Subject subject = getSubject();
		if (subject == null)
			return null;
		Principal principal = subject.getPrincipals().iterator().next();
		return principal.getName();

	}

	public final static Set<String> roles() {
		Principal principal = getSubject().getPrincipals().iterator().next();
		Authentication authentication = (Authentication) principal;
		Set<String> roles = Collections.synchronizedSet(new HashSet<String>());
		for (GrantedAuthority ga : authentication.getAuthorities()) {
			roles.add(ga.getAuthority());
		}
		return Collections.unmodifiableSet(roles);
	}

	public final static Subject getSubject() {

		Subject subject = Subject.getSubject(AccessController.getContext());
		if (subject == null)
			throw new ArgeoException("Not authenticated.");
		return subject;

	}
}
