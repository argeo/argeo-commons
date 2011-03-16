package org.argeo.security.jcr;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.argeo.ArgeoException;
import org.argeo.security.SiteAuthenticationToken;
import org.springframework.security.GrantedAuthority;

public class JcrAuthenticationToken extends SiteAuthenticationToken {
	private static final long serialVersionUID = -2736830165315486169L;
	private final transient Node userHome;

	public JcrAuthenticationToken(Object principal, Object credentials,
			GrantedAuthority[] authorities, String url, Node userHome) {
		super(principal, credentials, authorities, url,
				extractWorkspace(userHome));
		this.userHome = userHome;
	}

	private static String extractWorkspace(Node userHome) {
		try {
			return userHome.getSession().getWorkspace().getName();
		} catch (RepositoryException e) {
			throw new ArgeoException("Cannot extract workspace of " + userHome,
					e);
		}
	}

	public Node getUserHome() {
		return userHome;
	}

}
