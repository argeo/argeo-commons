package org.argeo.security.jcr;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.Credentials;
import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.RepositoryFactory;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.Value;

import org.argeo.ArgeoException;
import org.argeo.jcr.ArgeoJcrConstants;
import org.argeo.jcr.ArgeoNames;
import org.argeo.jcr.JcrUtils;
import org.argeo.security.SiteAuthenticationToken;
import org.springframework.security.Authentication;
import org.springframework.security.AuthenticationException;
import org.springframework.security.GrantedAuthority;
import org.springframework.security.GrantedAuthorityImpl;
import org.springframework.security.providers.AuthenticationProvider;
import org.springframework.security.userdetails.UserDetails;

/** Connects to a JCR repository and delegates authentication to it. */
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
			SimpleCredentials sp = new SimpleCredentials(siteAuth.getName(),
					siteAuth.getCredentials().toString().toCharArray());
			// get repository
			Repository repository = getRepository(url, sp);
			if (repository == null)
				return null;

			String workspace = siteAuth.getWorkspace();
			Session session;
			if (workspace == null || workspace.trim().equals(""))
				session = repository.login(sp);
			else
				session = repository.login(sp, workspace);

			Node userHome = JcrUtils.getUserHome(session);

			// retrieve remote roles
			Node userProfile = JcrUtils.getUserProfile(session);
			List<GrantedAuthority> authorities = new ArrayList<GrantedAuthority>();
			if (userProfile.hasProperty(ArgeoNames.ARGEO_REMOTE_ROLES)) {
				Value[] roles = userProfile.getProperty(
						ArgeoNames.ARGEO_REMOTE_ROLES).getValues();
				for (int i = 0; i < roles.length; i++)
					authorities.add(new GrantedAuthorityImpl(roles[i]
							.getString()));
			}
			JcrAuthenticationToken authen = new JcrAuthenticationToken(
					siteAuth.getPrincipal(),
					siteAuth.getCredentials(),
					authorities.toArray(new GrantedAuthority[authorities.size()]),
					url, userHome);
			authen.setDetails(getUserDetails(userHome, authen));

			return authen;
		} catch (RepositoryException e) {
			throw new ArgeoException(
					"Unexpected exception when authenticating to " + url, e);
		}
	}

	protected Repository getRepository(String url, Credentials credentials)
			throws RepositoryException {
		Map<String, String> parameters = new HashMap<String, String>();
		parameters.put(ArgeoJcrConstants.JCR_REPOSITORY_URI, url);
		return repositoryFactory.getRepository(parameters);
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
	protected UserDetails getUserDetails(Node userHome, Authentication authen) {
		try {
			// TODO: loads enabled, locked, etc. from the home node.
			return new JcrUserDetails(userHome.getPath(), authen.getPrincipal()
					.toString(), authen.getCredentials().toString(),
					isEnabled(userHome), true, true, true,
					authen.getAuthorities());
		} catch (Exception e) {
			throw new ArgeoException("Cannot get user details for " + userHome,
					e);
		}
	}

	protected Boolean isEnabled(Node userHome) {
		return true;
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
