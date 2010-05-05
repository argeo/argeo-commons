package org.argeo.security.core;

import org.springframework.security.GrantedAuthority;
import org.springframework.security.GrantedAuthorityImpl;
import org.springframework.security.adapters.PrincipalSpringSecurityUserToken;

public class InternalAuthentication extends PrincipalSpringSecurityUserToken {
	private static final long serialVersionUID = -6783376375615949315L;
	public final static String DEFAULT_SYSTEM_USERNAME = "system";
	public final static String DEFAULT_SYSTEM_ROLE = "ROLE_SYSTEM";

	public InternalAuthentication(String key, String systemUsername,
			String systemRole) {
		super(
				key,
				systemUsername,
				key,
				new GrantedAuthority[] { new GrantedAuthorityImpl(systemRole) },
				systemUsername);
	}

	public InternalAuthentication(String key) {
		this(key, DEFAULT_SYSTEM_USERNAME, DEFAULT_SYSTEM_ROLE);
	}

}
