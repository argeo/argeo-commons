package org.argeo.security.jcr;

import java.util.Map;
import java.util.concurrent.Executor;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.RepositoryFactory;
import javax.jcr.Session;

import org.argeo.ArgeoException;
import org.argeo.jcr.JcrUtils;
import org.argeo.security.OsAuthenticationToken;
import org.argeo.security.core.OsAuthenticationProvider;
import org.springframework.security.Authentication;
import org.springframework.security.AuthenticationException;
import org.springframework.security.userdetails.UserDetails;

public class OsJcrAuthenticationProvider extends OsAuthenticationProvider {
	private RepositoryFactory repositoryFactory;
	private Executor systemExecutor;
	private String homeBasePath = "/home";
	private String repositoryAlias = "node";
	private String workspace = null;

	public Authentication authenticate(Authentication authentication)
			throws AuthenticationException {
		final OsAuthenticationToken authen = (OsAuthenticationToken) super
				.authenticate(authentication);
		systemExecutor.execute(new Runnable() {
			public void run() {
				try {
					Session session = JcrUtils.getRepositoryByAlias(
							repositoryFactory, repositoryAlias)
							.login(workspace);
					Node userHome = JcrUtils.getUserHome(session,
							authen.getName());
					if (userHome == null)
						JcrUtils.createUserHome(session, homeBasePath,
								authen.getName());
					authen.setDetails(getUserDetails(userHome, authen));
				} catch (RepositoryException e) {
					throw new ArgeoException(
							"Unexpected exception when synchronizing OS and JCR security ",
							e);
				}
			}
		});
		return authen;
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

	public void register(RepositoryFactory repositoryFactory,
			Map<String, String> parameters) {
		this.repositoryFactory = repositoryFactory;
	}

	public void unregister(RepositoryFactory repositoryFactory,
			Map<String, String> parameters) {
		this.repositoryFactory = null;
	}

	public void setSystemExecutor(Executor systemExecutor) {
		this.systemExecutor = systemExecutor;
	}

	public void setHomeBasePath(String homeBasePath) {
		this.homeBasePath = homeBasePath;
	}

	public void setRepositoryAlias(String repositoryAlias) {
		this.repositoryAlias = repositoryAlias;
	}

	public void setWorkspace(String workspace) {
		this.workspace = workspace;
	}

}
