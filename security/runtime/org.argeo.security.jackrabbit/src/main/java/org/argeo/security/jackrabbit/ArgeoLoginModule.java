package org.argeo.security.jackrabbit;

import java.security.Principal;
import java.security.acl.Group;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import javax.jcr.Credentials;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.LoginException;

import org.apache.jackrabbit.core.security.AnonymousPrincipal;
import org.apache.jackrabbit.core.security.authentication.AbstractLoginModule;
import org.apache.jackrabbit.core.security.authentication.Authentication;
import org.apache.jackrabbit.core.security.principal.AdminPrincipal;
import org.argeo.security.SystemAuthentication;
import org.springframework.security.GrantedAuthority;
import org.springframework.security.context.SecurityContextHolder;
import org.springframework.security.providers.anonymous.AnonymousAuthenticationToken;

public class ArgeoLoginModule extends AbstractLoginModule {
	private String adminRole = "ROLE_ADMIN";

	/**
	 * Returns the Spring {@link org.springframework.security.Authentication}
	 * (which can be null)
	 */
	@Override
	protected Principal getPrincipal(Credentials credentials) {
		org.springframework.security.Authentication authen = SecurityContextHolder
				.getContext().getAuthentication();
		return authen;
	}

	protected Set<Principal> getPrincipals() {
		// use linked HashSet instead of HashSet in order to maintain the order
		// of principals (as in the Subject).
		Set<Principal> principals = new LinkedHashSet<Principal>();
		principals.add(principal);

		org.springframework.security.Authentication authen = (org.springframework.security.Authentication) principal;

		if (authen instanceof SystemAuthentication)
			principals.add(new AdminPrincipal(authen.getName()));
		else if (authen instanceof AnonymousAuthenticationToken)
			principals.add(new AnonymousPrincipal());
		else
			for (GrantedAuthority ga : authen.getAuthorities()) {
				if (adminRole.equals(ga.getAuthority()))
					principals.add(new AdminPrincipal(authen.getName()));
			}

		// override credentials since we did not used the one passed to us
		credentials = new SimpleCredentials(authen.getName(), authen
				.getCredentials().toString().toCharArray());

		return principals;
	}

	@SuppressWarnings("rawtypes")
	@Override
	protected void doInit(CallbackHandler callbackHandler, Session session,
			Map options) throws LoginException {
	}

	@Override
	protected boolean impersonate(Principal principal, Credentials credentials)
			throws RepositoryException, LoginException {
		throw new UnsupportedOperationException(
				"Impersonation is not yet supported");
	}

	@Override
	protected Authentication getAuthentication(final Principal principal,
			Credentials creds) throws RepositoryException {
		if (principal instanceof Group) {
			return null;
		}
		return new Authentication() {
			public boolean canHandle(Credentials credentials) {
				return principal instanceof org.springframework.security.Authentication;
			}

			public boolean authenticate(Credentials credentials)
					throws RepositoryException {
				return ((org.springframework.security.Authentication) principal)
						.isAuthenticated();
			}
		};
	}

}
