package org.argeo.security.equinox;

import java.security.Principal;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.security.auth.Subject;

import org.eclipse.equinox.security.auth.ILoginContext;
import org.eclipse.equinox.security.auth.LoginContextFactory;
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

	private final static ILoginContext getLoginContext() {
		return EquinoxSecurity.getLoginContext();
//		return LoginContextFactory
//				.createContext(EquinoxSecurity.CONTEXT_SPRING);
	}

	// private static void login() {
	// try {
	// getLoginContext().login();
	// } catch (LoginException e) {
	// throw new RuntimeException("Cannot login", e);
	// }
	// }

	public final static Subject getSubject() {

		Subject subject = null;
		// subject = Subject.getSubject(AccessController.getContext());
		try {
			getLoginContext().login();
			subject = getLoginContext().getSubject();
		} catch (Exception e) {
			throw new RuntimeException("Cannot retrieve subject", e);
		}

		return subject;

	}

}
