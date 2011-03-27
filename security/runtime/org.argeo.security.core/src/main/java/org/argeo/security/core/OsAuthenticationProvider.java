package org.argeo.security.core;

import java.security.AccessController;
import java.util.ArrayList;
import java.util.List;

import javax.security.auth.Subject;

import org.argeo.security.OsAuthenticationToken;
import org.springframework.security.Authentication;
import org.springframework.security.AuthenticationException;
import org.springframework.security.GrantedAuthority;
import org.springframework.security.GrantedAuthorityImpl;
import org.springframework.security.providers.AuthenticationProvider;
import org.springframework.security.providers.UsernamePasswordAuthenticationToken;

/** Validates an OS authentication. */
public class OsAuthenticationProvider implements AuthenticationProvider {
	private String osUserRole = "ROLE_OS_USER";
	private String userRole = "ROLE_USER";
	private String adminRole = "ROLE_ADMIN";

	private Boolean isAdmin = true;

	public Authentication authenticate(Authentication authentication)
			throws AuthenticationException {
		final OsAuthenticationToken oat;
		// if (authentication instanceof UsernamePasswordAuthenticationToken) {
		// Subject subject = Subject.getSubject(AccessController.getContext());
		// if (subject == null)
		// return null;
		// oat = new OsAuthenticationToken();
		// } else
		if (authentication instanceof OsAuthenticationToken) {
			oat = (OsAuthenticationToken) authentication;
		} else {
			return null;
		}

		// not OS authenticated
//		if (oat.getUser() == null)
//			return null;

		List<GrantedAuthority> auths = new ArrayList<GrantedAuthority>();
		auths.add(new GrantedAuthorityImpl(osUserRole));
		auths.add(new GrantedAuthorityImpl(userRole));
		if (isAdmin)
			auths.add(new GrantedAuthorityImpl(adminRole));
		return new OsAuthenticationToken(
				auths.toArray(new GrantedAuthority[auths.size()]));
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
