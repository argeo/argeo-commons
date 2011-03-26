package org.argeo.security.core;

import java.util.ArrayList;
import java.util.List;

import org.argeo.security.OsAuthenticationToken;
import org.springframework.security.Authentication;
import org.springframework.security.AuthenticationException;
import org.springframework.security.GrantedAuthority;
import org.springframework.security.GrantedAuthorityImpl;
import org.springframework.security.providers.AuthenticationProvider;

/** Validates an OS authentication. */
public class OsAuthenticationProvider implements AuthenticationProvider {
	private String osUserRole = "ROLE_OS_USER";
	private String userRole = "ROLE_USER";
	private String adminRole = "ROLE_ADMIN";

	private Boolean isAdmin = true;

	public Authentication authenticate(Authentication authentication)
			throws AuthenticationException {
		if (authentication instanceof OsAuthenticationToken) {
			List<GrantedAuthority> auths = new ArrayList<GrantedAuthority>();
			auths.add(new GrantedAuthorityImpl(osUserRole));
			auths.add(new GrantedAuthorityImpl(userRole));
			if (isAdmin)
				auths.add(new GrantedAuthorityImpl(adminRole));
			return new OsAuthenticationToken(
					auths.toArray(new GrantedAuthority[auths.size()]));
		}
		return null;
	}

	@SuppressWarnings("rawtypes")
	public boolean supports(Class authentication) {
		return OsAuthenticationToken.class.isAssignableFrom(authentication);
	}

	public void setOsUserRole(String osUserRole) {
		this.osUserRole = osUserRole;
	}

	public void setAdminRole(String adminRole) {
		this.adminRole = adminRole;
	}

	public void setIsAdmin(Boolean isAdmin) {
		this.isAdmin = isAdmin;
	}

}
