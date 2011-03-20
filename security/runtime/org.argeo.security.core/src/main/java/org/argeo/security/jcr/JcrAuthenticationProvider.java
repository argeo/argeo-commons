package org.argeo.security.jcr;

import java.util.HashMap;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.RepositoryFactory;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import org.argeo.ArgeoException;
import org.argeo.jcr.ArgeoJcrConstants;
import org.argeo.jcr.JcrUtils;
import org.argeo.security.SiteAuthenticationToken;
import org.springframework.security.Authentication;
import org.springframework.security.AuthenticationException;
import org.springframework.security.GrantedAuthority;
import org.springframework.security.GrantedAuthorityImpl;
import org.springframework.security.providers.AuthenticationProvider;
import org.springframework.security.userdetails.UserDetails;

/** Connects to a JCR repository and delegate authentication to it. */
public class JcrAuthenticationProvider implements AuthenticationProvider {
	public final static String ROLE_REMOTE_JCR_AUTHENTICATED = "ROLE_REMOTE_JCR_AUTHENTICATED";

	private RepositoryFactory repositoryFactory;

	public Authentication authenticate(Authentication authentication)
			throws AuthenticationException {
		if (!(authentication instanceof SiteAuthenticationToken))
			return null;
		SiteAuthenticationToken siteAuth = (SiteAuthenticationToken) authentication;
		String url = siteAuth.getUrl();
		if (url == null)
			return null;

		try {
			Map<String, String> parameters = new HashMap<String, String>();
			parameters.put(ArgeoJcrConstants.JCR_REPOSITORY_URI, url);

			Repository repository = null;
			repository = repositoryFactory.getRepository(parameters);
			if (repository == null)
				return null;

			SimpleCredentials sp = new SimpleCredentials(siteAuth.getName(),
					siteAuth.getCredentials().toString().toCharArray());
			String workspace = siteAuth.getWorkspace();
			Session session;
			if (workspace == null || workspace.trim().equals(""))
				session = repository.login(sp);
			else
				session = repository.login(sp, workspace);
			Node userHome = JcrUtils.getUserHome(session);
			if (userHome == null)
				throw new ArgeoException("No home found for user "
						+ session.getUserID());
			GrantedAuthority[] authorities = {};
			JcrAuthenticationToken authen = new JcrAuthenticationToken(
					siteAuth.getPrincipal(), siteAuth.getCredentials(),
					authorities, url, userHome);
			authen.setDetails(getUserDetails(userHome, authen));
			return authen;
		} catch (RepositoryException e) {
			throw new ArgeoException(
					"Unexpected exception when authenticating to " + url, e);
		}
	}

	/**
	 * By default, assigns only the role {@value #ROLE_REMOTE_JCR_AUTHENTICATED}
	 * . Should typically be overridden in order to assign more relevant roles.
	 */
	protected GrantedAuthority[] getGrantedAuthorities(Session session) {
		return new GrantedAuthority[] { new GrantedAuthorityImpl(
				ROLE_REMOTE_JCR_AUTHENTICATED) };
	}

	/** Builds user details based on the authentication and the user home. */
	protected UserDetails getUserDetails(Node userHome,
			JcrAuthenticationToken authen) {
		try {
			// TODO: loads enabled, locked, etc. from the home node.
			return new JcrUserDetails(userHome.getPath(), authen.getPrincipal()
					.toString(), authen.getCredentials().toString(), true,
					true, true, true, authen.getAuthorities());
		} catch (Exception e) {
			throw new ArgeoException("Cannot get user details for " + userHome,
					e);
		}
	}

	@SuppressWarnings("rawtypes")
	public boolean supports(Class authentication) {
		return SiteAuthenticationToken.class.isAssignableFrom(authentication);
	}

	public void register(RepositoryFactory repositoryFactory,
			Map<String, String> parameters) {
		this.repositoryFactory = repositoryFactory;
	}

	public void unregister(RepositoryFactory repositoryFactory,
			Map<String, String> parameters) {
		this.repositoryFactory = null;
	}
}
