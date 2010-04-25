package org.argeo.security.core;

import org.springframework.security.GrantedAuthority;
import org.springframework.security.GrantedAuthorityImpl;
import org.springframework.security.adapters.PrincipalSpringSecurityUserToken;

public class InternalAuthentication extends PrincipalSpringSecurityUserToken {
	private static final long serialVersionUID = -6783376375615949315L;
	private final static String SYSTEM_USERNAME = "system";
	private final static String SYSTEM_ROLE = "ROLE_SYSTEM";

	public InternalAuthentication(String key) {
		super(
				key,
				SYSTEM_USERNAME,
				key,
				new GrantedAuthority[] { new GrantedAuthorityImpl(SYSTEM_ROLE) },
				SYSTEM_USERNAME);
	}

}
