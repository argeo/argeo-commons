package org.argeo.security.jackrabbit;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.jackrabbit.core.security.authentication.CryptedSimpleCredentials;
import org.argeo.ArgeoException;
import org.argeo.jcr.JcrUtils;
import org.argeo.jcr.UserJcrUtils;
import org.argeo.security.UserAdminService;
import org.argeo.security.jcr.JcrSecurityModel;
import org.argeo.security.jcr.JcrUserDetails;
import org.springframework.dao.DataAccessException;
import org.springframework.security.Authentication;
import org.springframework.security.AuthenticationException;
import org.springframework.security.BadCredentialsException;
import org.springframework.security.GrantedAuthority;
import org.springframework.security.GrantedAuthorityImpl;
import org.springframework.security.context.SecurityContextHolder;
import org.springframework.security.providers.AuthenticationProvider;
import org.springframework.security.providers.UsernamePasswordAuthenticationToken;
import org.springframework.security.userdetails.UserDetails;
import org.springframework.security.userdetails.UsernameNotFoundException;

/**
 * An implementation of {@link UserAdminService} which closely wraps Jackrabbits
 * implementation. Roles are implemented with Groups.
 */
public class JackrabbitUserAdminService implements UserAdminService,
		AuthenticationProvider {
	private Repository repository;
	private JcrSecurityModel securityModel;

	private JackrabbitSession adminSession = null;

	private String superUsername = "root";
	private String superUserInitialPassword = "demo";

	public void init() throws RepositoryException {
		Authentication authentication = SecurityContextHolder.getContext()
				.getAuthentication();
		authentication.getName();
		adminSession = (JackrabbitSession) repository.login();
		Authorizable adminGroup = getUserManager()
				.getAuthorizable("ROLE_ADMIN");
		if (adminGroup == null) {
			adminGroup = getUserManager().createGroup("ROLE_ADMIN");
			adminSession.save();
		}
		Authorizable superUser = getUserManager()
				.getAuthorizable(superUsername);
		if (superUser == null) {
			superUser = getUserManager().createUser(superUsername,
					superUserInitialPassword);
			((Group) adminGroup).addMember(superUser);
			securityModel.sync(adminSession, superUsername, null);
			adminSession.save();
		}
	}

	public void destroy() throws RepositoryException {
		JcrUtils.logoutQuietly(adminSession);
	}

	private UserManager getUserManager() throws RepositoryException {
		return adminSession.getUserManager();
	}

	@Override
	public void createUser(UserDetails user) {
		try {
			getUserManager().createUser(user.getUsername(), user.getPassword());
			securityModel.sync(adminSession, user.getUsername(), null);
		} catch (RepositoryException e) {
			throw new ArgeoException("Cannot create user " + user, e);
		}
	}

	@Override
	public void updateUser(UserDetails user) {

	}

	@Override
	public void deleteUser(String username) {
		try {
			getUserManager().getAuthorizable(username).remove();
		} catch (RepositoryException e) {
			throw new ArgeoException("Cannot remove user " + username, e);
		}
	}

	@Override
	public void changePassword(String oldPassword, String newPassword) {
		Authentication authentication = SecurityContextHolder.getContext()
				.getAuthentication();
		try {
			SimpleCredentials sp = new SimpleCredentials(
					authentication.getName(), authentication.getCredentials()
							.toString().toCharArray());
			User user = (User) getUserManager().getAuthorizable(
					authentication.getName());
			CryptedSimpleCredentials credentials = (CryptedSimpleCredentials) user
					.getCredentials();
			if (credentials.matches(sp))
				user.changePassword(newPassword);
			else
				throw new BadCredentialsException("Bad credentials provided");
		} catch (Exception e) {
			throw new ArgeoException("Cannot change password for user "
					+ authentication.getName(), e);
		}
	}

	@Override
	public boolean userExists(String username) {
		try {
			Authorizable authorizable = getUserManager().getAuthorizable(
					username);
			if (authorizable != null && authorizable instanceof User)
				return true;
			return false;
		} catch (RepositoryException e) {
			throw new ArgeoException("Cannot check whether user " + username
					+ " exists ", e);
		}
	}

	@Override
	public Set<String> listUsers() {
		LinkedHashSet<String> res = new LinkedHashSet<String>();
		try {
			Iterator<Authorizable> users = getUserManager().findAuthorizables(
					null, null, UserManager.SEARCH_TYPE_USER);
			while (users.hasNext()) {
				res.add(users.next().getPrincipal().getName());
			}
			return res;
		} catch (RepositoryException e) {
			throw new ArgeoException("Cannot list users", e);
		}
	}

	@Override
	public Set<String> listUsersInRole(String role) {
		LinkedHashSet<String> res = new LinkedHashSet<String>();
		try {
			Group group = (Group) getUserManager().getAuthorizable(role);
			Iterator<Authorizable> users = group.getMembers();
			// NB: not recursive
			while (users.hasNext()) {
				res.add(users.next().getPrincipal().getName());
			}
			return res;
		} catch (RepositoryException e) {
			throw new ArgeoException("Cannot list users in role " + role, e);
		}
	}

	@Override
	public void synchronize() {
	}

	@Override
	public void newRole(String role) {
		try {
			getUserManager().createGroup(role);
		} catch (RepositoryException e) {
			throw new ArgeoException("Cannot create role " + role, e);
		}
	}

	@Override
	public Set<String> listEditableRoles() {
		LinkedHashSet<String> res = new LinkedHashSet<String>();
		try {
			Iterator<Authorizable> groups = getUserManager().findAuthorizables(
					null, null, UserManager.SEARCH_TYPE_GROUP);
			while (groups.hasNext()) {
				res.add(groups.next().getPrincipal().getName());
			}
			return res;
		} catch (RepositoryException e) {
			throw new ArgeoException("Cannot list groups", e);
		}
	}

	@Override
	public void deleteRole(String role) {
		try {
			getUserManager().getAuthorizable(role).remove();
		} catch (RepositoryException e) {
			throw new ArgeoException("Cannot remove role " + role, e);
		}
	}

	@Override
	public UserDetails loadUserByUsername(String username)
			throws UsernameNotFoundException, DataAccessException {
		try {
			User user = (User) getUserManager().getAuthorizable(username);
			return loadJcrUserDetails(adminSession, username,
					user.getCredentials());
		} catch (RepositoryException e) {
			throw new ArgeoException("Cannot load user " + username, e);
		}
	}

	protected JcrUserDetails loadJcrUserDetails(Session session,
			String username, Object credentials) throws RepositoryException {
		if (username == null)
			username = session.getUserID();
		User user = (User) getUserManager().getAuthorizable(username);
		ArrayList<GrantedAuthority> authorities = new ArrayList<GrantedAuthority>();
		// FIXME make it more generic
		authorities.add(new GrantedAuthorityImpl("ROLE_USER"));
		Iterator<Group> groups = user.declaredMemberOf();
		while (groups.hasNext()) {
			Group group = groups.next();
			// String role = "ROLE_"
			// + group.getPrincipal().getName().toUpperCase();
			String role = group.getPrincipal().getName();
			authorities.add(new GrantedAuthorityImpl(role));
		}

		Node userProfile = UserJcrUtils.getUserProfile(session, username);
		JcrUserDetails userDetails = new JcrUserDetails(userProfile,
				credentials.toString(),
				authorities.toArray(new GrantedAuthority[authorities.size()]));
		return userDetails;
	}

	// AUTHENTICATION PROVIDER
	public synchronized Authentication authenticate(
			Authentication authentication) throws AuthenticationException {
		UsernamePasswordAuthenticationToken siteAuth = (UsernamePasswordAuthenticationToken) authentication;
		String username = siteAuth.getName();
		try {
			SimpleCredentials sp = new SimpleCredentials(siteAuth.getName(),
					siteAuth.getCredentials().toString().toCharArray());
			User user = (User) getUserManager().getAuthorizable(username);
			CryptedSimpleCredentials credentials = (CryptedSimpleCredentials) user
					.getCredentials();
			// String providedPassword = siteAuth.getCredentials().toString();
			if (!credentials.matches(sp)) {
				throw new BadCredentialsException("Passwords do not match");
			}
			// session = repository.login(sp, null);

			Node userProfile = UserJcrUtils.getUserProfile(adminSession,
					username);
			JcrUserDetails.checkAccountStatus(userProfile);
		} catch (Exception e) {
			throw new BadCredentialsException(
					"Cannot authenticate " + siteAuth, e);
		}

		try {
			JcrUserDetails userDetails = loadJcrUserDetails(adminSession,
					username, siteAuth.getCredentials());
			UsernamePasswordAuthenticationToken authenticated = new UsernamePasswordAuthenticationToken(
					siteAuth, "", userDetails.getAuthorities());
			authenticated.setDetails(userDetails);
			return authenticated;
		} catch (RepositoryException e) {
			throw new ArgeoException(
					"Unexpected exception when authenticating " + siteAuth, e);
		}
	}

	@SuppressWarnings("rawtypes")
	public boolean supports(Class authentication) {
		return UsernamePasswordAuthenticationToken.class
				.isAssignableFrom(authentication);
	}

	public void setRepository(Repository repository) {
		this.repository = repository;
	}

	public void setSecurityModel(JcrSecurityModel securityModel) {
		this.securityModel = securityModel;
	}

}
