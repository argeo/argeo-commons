package org.argeo.security.jcr;

import java.util.Map;
import java.util.concurrent.Executor;

import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.argeo.ArgeoException;
import org.argeo.jcr.JcrUtils;
import org.argeo.security.OsAuthenticationToken;
import org.argeo.security.SystemExecutionService;
import org.argeo.security.core.OsAuthenticationProvider;
import org.springframework.security.Authentication;
import org.springframework.security.AuthenticationException;
import org.springframework.security.userdetails.UserDetails;

public class OsJcrAuthenticationProvider extends OsAuthenticationProvider {
	private Executor systemExecutor;
	private String homeBasePath = "/home";
	private Repository repository;
	private String workspace = null;

	private Long timeout = 5 * 60 * 1000l;

	public Authentication authenticate(Authentication authentication)
			throws AuthenticationException {
		final OsAuthenticationToken authen = (OsAuthenticationToken) super
				.authenticate(authentication);
		final Repository repository = getRepositoryBlocking();
		systemExecutor.execute(new Runnable() {
			public void run() {
				try {
					Session session = repository.login(workspace);
					// WARNING: at this stage we assume that teh java properties
					// will have the same value
					String userName = System.getProperty("user.name");
					Node userHome = JcrUtils.getUserHome(session, userName);
					if (userHome == null)
						userHome = JcrUtils.createUserHome(session,
								homeBasePath, userName);
					//authen.setDetails(getUserDetails(userHome, authen));
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

	protected Repository getRepositoryBlocking() {
		long begin = System.currentTimeMillis();
		while (repository == null) {
			synchronized (this) {
				try {
					wait(500);
				} catch (InterruptedException e) {
					// silent
				}
			}
			if (System.currentTimeMillis() - begin > timeout)
				throw new ArgeoException("No repository registered after "
						+ timeout + " ms");
		}
		return repository;
	}

	public synchronized void register(Repository repository,
			Map<String, String> parameters) {
		this.repository = repository;
		notifyAll();
	}

	public synchronized void unregister(Repository repository,
			Map<String, String> parameters) {
		this.repository = null;
		notifyAll();
	}

	public void register(SystemExecutionService systemExecutor,
			Map<String, String> parameters) {
		this.systemExecutor = systemExecutor;
	}

	public void unregister(SystemExecutionService systemExecutor,
			Map<String, String> parameters) {
		this.systemExecutor = null;
	}

	public void setHomeBasePath(String homeBasePath) {
		this.homeBasePath = homeBasePath;
	}

	public void setWorkspace(String workspace) {
		this.workspace = workspace;
	}

}
