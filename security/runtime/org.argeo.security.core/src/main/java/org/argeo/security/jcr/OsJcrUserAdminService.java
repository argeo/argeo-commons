package org.argeo.security.jcr;

import java.util.HashSet;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.argeo.ArgeoException;
import org.argeo.jcr.JcrUtils;
import org.argeo.security.UserAdminService;
import org.springframework.dao.DataAccessException;
import org.springframework.security.userdetails.UserDetails;
import org.springframework.security.userdetails.UsernameNotFoundException;

/**
 * Dummy user service to be used when running as a single OS user (typically
 * desktop). TODO integrate with JCR user / groups
 */
public class OsJcrUserAdminService implements UserAdminService {
	private String securityWorkspace = "security";
	private Repository repository;

	private Session securitySession;

	public void init() {
		try {
			securitySession = repository.login(securityWorkspace);
		} catch (RepositoryException e) {
			throw new ArgeoException("Cannot initialize", e);
		}
	}

	public void destroy() {
		JcrUtils.logoutQuietly(securitySession);
	}

	/** <b>Unsupported</b> */
	public void createUser(UserDetails user) {
		throw new UnsupportedOperationException();
	}

	/** Does nothing */
	public void updateUser(UserDetails user) {

	}

	/** <b>Unsupported</b> */
	public void deleteUser(String username) {
		throw new UnsupportedOperationException();
	}

	/** <b>Unsupported</b> */
	public void changePassword(String oldPassword, String newPassword) {
		throw new UnsupportedOperationException();
	}

	public boolean userExists(String username) {
		if (getSPropertyUsername().equals(username))
			return true;
		else
			return false;
	}

	public UserDetails loadUserByUsername(String username)
			throws UsernameNotFoundException, DataAccessException {
		if (getSPropertyUsername().equals(username)) {
			Node userProfile = JcrUtils.getUserProfile(securitySession,
					username);
			JcrUserDetails userDetails;
			try {
				userDetails = new JcrUserDetails(userProfile, "",
						OsJcrAuthenticationProvider.getBaseAuthorities());
			} catch (RepositoryException e) {
				throw new ArgeoException("Cannot retrieve user profile for "
						+ username, e);
			}
			return userDetails;
		} else {
			throw new UnsupportedOperationException();
		}
	}

	protected final String getSPropertyUsername() {
		return System.getProperty("user.name");
	}

	public Set<String> listUsers() {
		Set<String> set = new HashSet<String>();
		set.add(getSPropertyUsername());
		return set;
	}

	public Set<String> listUsersInRole(String role) {
		Set<String> set = new HashSet<String>();
		set.add(getSPropertyUsername());
		return set;
	}

	/** Does nothing */
	public void synchronize() {
	}

	/** <b>Unsupported</b> */
	public void newRole(String role) {
		throw new UnsupportedOperationException();
	}

	public Set<String> listEditableRoles() {
		Set<String> set = new HashSet<String>();
		return set;
	}

	/** <b>Unsupported</b> */
	public void deleteRole(String role) {
		throw new UnsupportedOperationException();
	}

	public void setRepository(Repository repository) {
		this.repository = repository;
	}

	public void setSecurityWorkspace(String securityWorkspace) {
		this.securityWorkspace = securityWorkspace;
	}

}
