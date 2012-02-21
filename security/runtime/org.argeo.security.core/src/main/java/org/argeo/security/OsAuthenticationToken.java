package org.argeo.security;

import java.security.AccessController;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import javax.security.auth.Subject;

import org.argeo.ArgeoException;
import org.argeo.OperatingSystem;
import org.springframework.security.Authentication;
import org.springframework.security.GrantedAuthority;
import org.springframework.security.GrantedAuthorityImpl;
import org.springframework.security.userdetails.UserDetails;

/** Abstracts principals provided by com.sun.security.auth.module login modules. */
public class OsAuthenticationToken implements Authentication {
	private static final long serialVersionUID = -7544626794250917244L;

	final Class<? extends Principal> osUserPrincipalClass;
	final Class<? extends Principal> osUserIdPrincipalClass;
	final Class<? extends Principal> osGroupIdPrincipalClass;

	private List<GrantedAuthority> grantedAuthorities;

	private UserDetails details;

	/** Request */
	public OsAuthenticationToken(GrantedAuthority[] grantedAuthorities) {
		this.grantedAuthorities = grantedAuthorities != null ? Arrays
				.asList(grantedAuthorities) : null;
		ClassLoader cl = getClass().getClassLoader();
		switch (OperatingSystem.os) {
		case OperatingSystem.WINDOWS:
			osUserPrincipalClass = getPrincipalClass(cl,
					"com.sun.security.auth.NTUserPrincipal");
			osUserIdPrincipalClass = getPrincipalClass(cl,
					"com.sun.security.auth.NTSidUserPrincipal");
			osGroupIdPrincipalClass = getPrincipalClass(cl,
					"com.sun.security.auth.NTSidGroupPrincipal");
			break;
		case OperatingSystem.NIX:
			osUserPrincipalClass = getPrincipalClass(cl,
					"com.sun.security.auth.UnixPrincipal");
			osUserIdPrincipalClass = getPrincipalClass(cl,
					"com.sun.security.auth.UnixNumericUserPrincipal");
			osGroupIdPrincipalClass = getPrincipalClass(cl,
					"com.sun.security.auth.UnixNumericGroupPrincipal");
			break;
		case OperatingSystem.SOLARIS:
			osUserPrincipalClass = getPrincipalClass(cl,
					"com.sun.security.auth.SolarisPrincipal");
			osUserIdPrincipalClass = getPrincipalClass(cl,
					"com.sun.security.auth.SolarisNumericUserPrincipal");
			osGroupIdPrincipalClass = getPrincipalClass(cl,
					"com.sun.security.auth.SolarisNumericGroupPrincipal");
			break;

		default:
			throw new ArgeoException("Unsupported operating system "
					+ OperatingSystem.os);
		}

	}

	/** Authenticated */
	public OsAuthenticationToken() {
		this(null);
	}

	/** @return the name, or null if not yet logged */
	public String getName() {
		Subject subject = Subject.getSubject(AccessController.getContext());
		if (subject == null)
			return null;
		return getUser().getName();
	}

	/**
	 * Should not be called during authentication since group IDs are not yet
	 * available {@link Subject} has been set
	 */
	public GrantedAuthority[] getAuthorities() {
		// grantedAuthorities should not be null at this stage
		List<GrantedAuthority> gas = new ArrayList<GrantedAuthority>(
				grantedAuthorities);
		for (Principal groupPrincipal : getGroupsIds()) {
			gas.add(new GrantedAuthorityImpl("OSGROUP_"
					+ groupPrincipal.getName()));
		}
		return gas.toArray(new GrantedAuthority[gas.size()]);
	}

	public UserDetails getDetails() {
		return details;
	}

	public void setDetails(UserDetails details) {
		this.details = details;
	}

	public boolean isAuthenticated() {
		return grantedAuthorities != null;
	}

	public void setAuthenticated(boolean isAuthenticated)
			throws IllegalArgumentException {
		if (grantedAuthorities != null)
			grantedAuthorities.clear();
		grantedAuthorities = null;
	}

	@SuppressWarnings("unchecked")
	protected static Class<? extends Principal> getPrincipalClass(
			ClassLoader cl, String className) {
		try {
			return (Class<? extends Principal>) cl.loadClass(className);
		} catch (ClassNotFoundException e) {
			throw new ArgeoException("Cannot load principal class", e);
		}
	}

	public Object getPrincipal() {
		return getUser();
	}

	public Principal getUser() {
		Subject subject = getSubject();
		Set<? extends Principal> userPrincipals = subject
				.getPrincipals(osUserPrincipalClass);
		if (userPrincipals == null || userPrincipals.size() == 0)
			throw new ArgeoException("No OS principal");
		if (userPrincipals.size() > 1)
			throw new ArgeoException("More than one OS principal");
		Principal user = userPrincipals.iterator().next();
		return user;
	}

	public Principal getUserId() {
		Subject subject = getSubject();
		Set<? extends Principal> userIdsPrincipals = subject
				.getPrincipals(osUserIdPrincipalClass);
		if (userIdsPrincipals == null || userIdsPrincipals.size() == 0)
			throw new ArgeoException("No user id principal");
		if (userIdsPrincipals.size() > 1)
			throw new ArgeoException("More than one user id principal");
		Principal userId = userIdsPrincipals.iterator().next();
		return userId;
	}

	public Set<? extends Principal> getGroupsIds() {
		Subject subject = getSubject();
		return (Set<? extends Principal>) subject
				.getPrincipals(osGroupIdPrincipalClass);
	}

	/** @return the subject always non null */
	protected Subject getSubject() {
		Subject subject = Subject.getSubject(AccessController.getContext());
		if (subject == null)
			throw new ArgeoException("No subject in JAAS context");
		return subject;
	}

	public Object getCredentials() {
		return "";
	}

}
