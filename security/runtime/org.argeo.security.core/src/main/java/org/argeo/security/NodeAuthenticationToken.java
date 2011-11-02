package org.argeo.security;

import org.springframework.security.GrantedAuthority;
import org.springframework.security.providers.UsernamePasswordAuthenticationToken;

/** Credentials required for the authentication to a node. */
public class NodeAuthenticationToken extends
		UsernamePasswordAuthenticationToken {
	private static final long serialVersionUID = 1955222132884795213L;
	private final String url;
	private final String securityWorkspace;

	/** Non authenticated local constructor */
	public NodeAuthenticationToken(Object principal, Object credentials) {
		super(principal, credentials);
		this.url = null;
		this.securityWorkspace = null;
	}

	/** Non authenticated remote constructor */
	public NodeAuthenticationToken(Object principal, Object credentials,
			String url, String workspace) {
		super(principal, credentials);
		this.url = url;
		this.securityWorkspace = workspace;
	}

	/** Authenticated constructor */
	public NodeAuthenticationToken(NodeAuthenticationToken sat,
			GrantedAuthority[] authorities) {
		super(sat.getPrincipal(), sat.getCredentials(), authorities);
		this.url = sat.getUrl();
		this.securityWorkspace = sat.getSecurityWorkspace();
	}

	public String getUrl() {
		return url;
	}

	public String getSecurityWorkspace() {
		return securityWorkspace;
	}

	public Boolean isRemote() {
		return url != null;
	}
}
