package org.argeo.security;

import org.springframework.security.GrantedAuthority;
import org.springframework.security.providers.UsernamePasswordAuthenticationToken;

public class SiteAuthenticationToken extends
		UsernamePasswordAuthenticationToken {
	private static final long serialVersionUID = 1955222132884795213L;
	private final String url;
	private final String workspace;

	public SiteAuthenticationToken(Object principal, Object credentials,
			String url, String workspace) {
		super(principal, credentials);
		this.url = url;
		this.workspace = workspace;
	}

	public SiteAuthenticationToken(Object principal, Object credentials,
			GrantedAuthority[] authorities, String url, String workspace) {
		super(principal, credentials, authorities);
		this.url = url;
		this.workspace = workspace;
	}

	public String getUrl() {
		return url;
	}

	public String getWorkspace() {
		return workspace;
	}

}
