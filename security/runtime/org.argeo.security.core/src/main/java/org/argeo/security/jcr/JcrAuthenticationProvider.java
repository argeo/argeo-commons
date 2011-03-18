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
import org.argeo.jcr.ArgeoNames;
import org.argeo.jcr.ArgeoTypes;
import org.argeo.security.SiteAuthenticationToken;
import org.springframework.security.Authentication;
import org.springframework.security.AuthenticationException;
import org.springframework.security.GrantedAuthority;
import org.springframework.security.GrantedAuthorityImpl;
import org.springframework.security.providers.AuthenticationProvider;

/** Connects to a JCR repository and delegate authentication to it. */
public class JcrAuthenticationProvider implements AuthenticationProvider {
	private RepositoryFactory repositoryFactory;
	private final String defaultHome;
	private final String userRole;

	public JcrAuthenticationProvider() {
		this("ROLE_USER", "home");
	}

	public JcrAuthenticationProvider(String userRole) {
		this(userRole, "home");
	}

	public JcrAuthenticationProvider(String defaultHome, String userRole) {
		super();
		this.defaultHome = defaultHome;
		this.userRole = userRole;
	}

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
			Node userHome = getUserHome(session);
			GrantedAuthority[] authorities = {};
			return new JcrAuthenticationToken(siteAuth.getPrincipal(),
					siteAuth.getCredentials(), authorities, url, userHome);
		} catch (RepositoryException e) {
			throw new ArgeoException(
					"Unexpected exception when authenticating to " + url, e);
		}
	}

	protected GrantedAuthority[] getGrantedAuthorities(Session session) {
		return new GrantedAuthority[] { new GrantedAuthorityImpl(userRole) };
	}

	@SuppressWarnings("rawtypes")
	public boolean supports(Class authentication) {
		return SiteAuthenticationToken.class.isAssignableFrom(authentication);
	}

	protected Node getUserHome(Session session) {
		String userID = "<not yet logged in>";
		try {
			userID = session.getUserID();
			Node rootNode = session.getRootNode();
			Node homeNode;
			if (!rootNode.hasNode(defaultHome)) {
				homeNode = rootNode.addNode(defaultHome, ArgeoTypes.ARGEO_HOME);
			} else {
				homeNode = rootNode.getNode(defaultHome);
			}

			Node userHome;
			if (!homeNode.hasNode(userID)) {
				userHome = homeNode.addNode(userID);
				userHome.addMixin(ArgeoTypes.ARGEO_USER_HOME);
				userHome.setProperty(ArgeoNames.ARGEO_USER_ID, userID);
			} else {
				userHome = homeNode.getNode(userID);
			}
			session.save();
			return userHome;
		} catch (Exception e) {
			throw new ArgeoException("Cannot initialize home for user '"
					+ userID + "'", e);
		}
	}

	public void register(RepositoryFactory repositoryFactory,
			Map<String, String> parameters) {
		this.repositoryFactory = repositoryFactory;
	}

	public void unregister(RepositoryFactory repositoryFactory,
			Map<String, String> parameters) {
		this.repositoryFactory = null;
	}

	public String getDefaultHome() {
		return defaultHome;
	}

	public String getUserRole() {
		return userRole;
	}

}
