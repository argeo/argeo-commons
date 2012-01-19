package org.argeo.security.jcr;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.argeo.jcr.ArgeoNames;
import org.argeo.jcr.JcrUtils;
import org.springframework.security.BadCredentialsException;
import org.springframework.security.DisabledException;
import org.springframework.security.GrantedAuthority;
import org.springframework.security.GrantedAuthorityImpl;
import org.springframework.security.LockedException;
import org.springframework.security.userdetails.User;

/** User details based on a user profile node. */
public class JcrUserDetails extends User implements ArgeoNames {
	private static final long serialVersionUID = -8142764995842559646L;
	private final String homePath;
	private final String securityWorkspace;

	protected JcrUserDetails(String securityWorkspace, String homePath,
			String username, String password, boolean enabled,
			boolean accountNonExpired, boolean credentialsNonExpired,
			boolean accountNonLocked, GrantedAuthority[] authorities)
			throws IllegalArgumentException {
		super(username, password, enabled, accountNonExpired,
				credentialsNonExpired, accountNonLocked, authorities);
		this.homePath = homePath;
		this.securityWorkspace = securityWorkspace;
	}

	public JcrUserDetails(Node userProfile, String password,
			GrantedAuthority[] authorities) throws RepositoryException {
		super(
				userProfile.getProperty(ARGEO_USER_ID).getString(),
				password,
				userProfile.getProperty(ARGEO_ENABLED).getBoolean(),
				userProfile.getProperty(ARGEO_ACCOUNT_NON_EXPIRED).getBoolean(),
				userProfile.getProperty(ARGEO_CREDENTIALS_NON_EXPIRED)
						.getBoolean(), userProfile.getProperty(
						ARGEO_ACCOUNT_NON_LOCKED).getBoolean(), authorities);
		// home is defined as the parent of the profile
		homePath = userProfile.getParent().getPath();
		securityWorkspace = userProfile.getSession().getWorkspace().getName();
	}

	/**
	 * Convenience constructor
	 * 
	 * @param session
	 *            the security session
	 * @param username
	 *            the username
	 * @param password
	 *            the password, can be null
	 * @param authorities
	 *            the granted authorities
	 */
	public JcrUserDetails(Session session, String username, String password,
			GrantedAuthority[] authorities) throws RepositoryException {
		this(JcrUtils.getUserProfile(session, username),
				password != null ? password : "", authorities);
	}

	/**
	 * Check the account status in JCR, throwing the exceptions expected by
	 * Spring security if needed.
	 */
	public static void checkAccountStatus(Node userProfile) {
		try {
			if (!userProfile.getProperty(ARGEO_ENABLED).getBoolean())
				throw new DisabledException(userProfile.getPath()
						+ " is disabled");
			if (!userProfile.getProperty(ARGEO_ACCOUNT_NON_LOCKED).getBoolean())
				throw new LockedException(userProfile.getPath() + " is locked");
		} catch (RepositoryException e) {
			throw new BadCredentialsException("Cannot check account status", e);
		}
	}

	/** Clone immutable with new roles */
	public JcrUserDetails cloneWithNewRoles(List<String> roles) {
		List<GrantedAuthority> authorities = new ArrayList<GrantedAuthority>();
		for (String role : roles) {
			authorities.add(new GrantedAuthorityImpl(role));
		}
		return new JcrUserDetails(securityWorkspace, homePath, getUsername(),
				getPassword(), isEnabled(), isAccountNonExpired(),
				isAccountNonExpired(), isAccountNonLocked(),
				authorities.toArray(new GrantedAuthority[authorities.size()]));
	}

	/** Clone immutable with new password */
	public JcrUserDetails cloneWithNewPassword(String password) {
		return new JcrUserDetails(securityWorkspace, homePath, getUsername(),
				password, isEnabled(), isAccountNonExpired(),
				isAccountNonExpired(), isAccountNonLocked(), getAuthorities());
	}

	public String getHomePath() {
		return homePath;
	}

	/** Not yet API */
	public String getSecurityWorkspace() {
		return securityWorkspace;
	}
}
