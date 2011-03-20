package org.argeo.security.jcr;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.argeo.ArgeoException;
import org.argeo.security.SiteAuthenticationToken;
import org.springframework.security.GrantedAuthority;

/** An authenticated authentication based on a JCR session. */
public class JcrAuthenticationToken extends SiteAuthenticationToken {
	private static final long serialVersionUID = -2736830165315486169L;

	private final transient Session session;
	private final String userHomePath;

	public JcrAuthenticationToken(Object principal, Object credentials,
			GrantedAuthority[] authorities, String url, Node userHome) {
		super(principal, credentials, authorities, url,
				extractWorkspace(userHome));
		try {
			this.session = userHome.getSession();
			this.userHomePath = userHome.getPath();
		} catch (RepositoryException e) {
			throw new ArgeoException("Cannot extract path from " + userHome, e);
		}
	}

	private static String extractWorkspace(Node userHome) {
		try {
			return userHome.getSession().getWorkspace().getName();
		} catch (RepositoryException e) {
			throw new ArgeoException("Cannot extract workspace from "
					+ userHome, e);
		}
	}

	/** The path to the authenticated user home node. */
	public String getUserHomePath() {
		return userHomePath;
	}

	/** The session used to create this authentication. */
	public Session getSession() {
		return session;
	}

	@Override
	public boolean isAuthenticated() {
		if (session == null || !session.isLive())
			setAuthenticated(false);
		return super.isAuthenticated();
	}

	@Override
	public void setAuthenticated(boolean isAuthenticated)
			throws IllegalArgumentException {
		super.setAuthenticated(isAuthenticated);
		if (!isAuthenticated && session != null)
			session.logout();
	}

}
