package org.argeo.security.core;

import java.util.ArrayList;
import java.util.List;

import org.argeo.security.OsAuthenticationToken;
import org.springframework.security.Authentication;
import org.springframework.security.AuthenticationException;
import org.springframework.security.GrantedAuthority;
import org.springframework.security.GrantedAuthorityImpl;
import org.springframework.security.providers.AuthenticationProvider;

/**
 * Validates an OS authentication. The id is that it will always be
 * authenticated since we are always runnign within an OS, but the fact that the
 * {@link Authentication} works properly depends on the proper OS login module
 * having been called as well.
 */
public class OsAuthenticationProvider implements AuthenticationProvider {
	private String osUserRole = "ROLE_OS_USER";
	private String userRole = "ROLE_USER";
	private String adminRole = "ROLE_ADMIN";

	private Boolean isAdmin = true;

	public Authentication authenticate(Authentication authentication)
			throws AuthenticationException {
		return new OsAuthenticationToken(getBaseAuthorities());
	}

	protected GrantedAuthority[] getBaseAuthorities() {
		List<GrantedAuthority> auths = new ArrayList<GrantedAuthority>();
		auths.add(new GrantedAuthorityImpl(osUserRole));
		auths.add(new GrantedAuthorityImpl(userRole));
		if (isAdmin)
			auths.add(new GrantedAuthorityImpl(adminRole));
		return auths.toArray(new GrantedAuthority[auths.size()]);
	}

	@SuppressWarnings("rawtypes")
	public boolean supports(Class authentication) {
		return OsAuthenticationToken.class.isAssignableFrom(authentication);
	}

	public void setOsUserRole(String osUserRole) {
		this.osUserRole = osUserRole;
	}

	public void setUserRole(String userRole) {
		this.userRole = userRole;
	}

	public void setAdminRole(String adminRole) {
		this.adminRole = adminRole;
	}

	public void setIsAdmin(Boolean isAdmin) {
		this.isAdmin = isAdmin;
	}

}
