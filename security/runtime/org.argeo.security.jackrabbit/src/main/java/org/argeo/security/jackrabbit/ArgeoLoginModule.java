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

/** Jackrabbit login mechanism based on Spring Security */
public class ArgeoLoginModule extends AbstractLoginModule {
	private String adminRole = "ROLE_ADMIN";

	@Override
	public boolean login() throws LoginException {
		boolean loginOk = super.login();
		if (!loginOk) {
			org.springframework.security.Authentication authen = (org.springframework.security.Authentication) SecurityContextHolder
					.getContext().getAuthentication();
		}
		return loginOk;
	}

	@Override
	public boolean commit() throws LoginException {
		boolean commitOk = super.commit();
		if (!commitOk) {
			org.springframework.security.Authentication authen = (org.springframework.security.Authentication) SecurityContextHolder
					.getContext().getAuthentication();
		}
		return commitOk;
	}

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
		// clear already registered Jackrabbit principals
		// clearPrincipals(AdminPrincipal.class);
		// clearPrincipals(AnonymousPrincipal.class);
		// clearPrincipals(GrantedAuthorityPrincipal.class);

		return syncPrincipals();
	}

	protected Set<Principal> syncPrincipals() {
		// use linked HashSet instead of HashSet in order to maintain the order
		// of principals (as in the Subject).
		org.springframework.security.Authentication authen = (org.springframework.security.Authentication) principal;

		Set<Principal> principals = new LinkedHashSet<Principal>();
		principals.add(authen);

		if (authen instanceof SystemAuthentication) {
			principals.add(new AdminPrincipal(authen.getName()));
			principals.add(new ArgeoSystemPrincipal(authen.getName()));
		} else if (authen instanceof AnonymousAuthenticationToken) {
			principals.add(new AnonymousPrincipal());
		} else {
			for (GrantedAuthority ga : authen.getAuthorities()) {
				principals.add(new GrantedAuthorityPrincipal(ga));
				// FIXME: make it more generic
				if (adminRole.equals(ga.getAuthority()))
					principals.add(new AdminPrincipal(authen.getName()));
			}
		}

		// remove previous credentials
		Set<SimpleCredentials> thisCredentials = subject
				.getPublicCredentials(SimpleCredentials.class);
		if (thisCredentials != null)
			thisCredentials.clear();
		// override credentials since we did not used the one passed to us
		// credentials = new SimpleCredentials(authen.getName(), authen
		// .getCredentials().toString().toCharArray());

		return principals;
	}

	/**
	 * Super implementation removes all {@link Principal}, the Spring
	 * {@link org.springframework.security.Authentication} as well. Here we
	 * simply clear Jackrabbit related {@link Principal}s.
	 */
	@Override
	public boolean logout() throws LoginException {
		clearPrincipals(AdminPrincipal.class);
		clearPrincipals(ArgeoSystemPrincipal.class);
		clearPrincipals(AnonymousPrincipal.class);
		clearPrincipals(GrantedAuthorityPrincipal.class);

		// we resync with Spring Security since the subject may have been reused
		// in beetween
		// TODO: check if this is clean
		// subject.getPrincipals().addAll(syncPrincipals());

		return true;
	}

	private <T extends Principal> void clearPrincipals(Class<T> clss) {
		Set<T> principals = subject.getPrincipals(clss);
		if (principals != null)
			principals.clear();
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
