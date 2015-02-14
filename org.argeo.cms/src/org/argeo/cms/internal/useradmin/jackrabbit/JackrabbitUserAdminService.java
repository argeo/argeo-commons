package org.argeo.cms.internal.useradmin.jackrabbit;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.version.VersionManager;

import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.jackrabbit.core.security.authentication.CryptedSimpleCredentials;
import org.argeo.ArgeoException;
import org.argeo.cms.internal.auth.GrantedAuthorityPrincipal;
import org.argeo.cms.internal.auth.JcrSecurityModel;
import org.argeo.jcr.JcrUtils;
import org.argeo.jcr.UserJcrUtils;
import org.argeo.security.NodeAuthenticationToken;
import org.argeo.security.UserAdminService;
import org.argeo.security.jcr.JcrUserDetails;
import org.argeo.security.jcr.NewUserDetails;
import org.springframework.dao.DataAccessException;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

/**
 * An implementation of {@link UserAdminService} which closely wraps Jackrabbits
 * implementation. Roles are implemented with Groups.
 */
public class JackrabbitUserAdminService implements UserAdminService,
		AuthenticationProvider {
	final static String userRole = "ROLE_USER";
	final static String adminRole = "ROLE_ADMIN";

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
		Authorizable adminGroup = getUserManager().getAuthorizable(adminRole);
		if (adminGroup == null) {
			adminGroup = getUserManager().createGroup(adminRole);
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
			// if (getUserManager().getAuthorizable(user.getUsername()) == null)
			// {
			getUserManager().createUser(user.getUsername(), user.getPassword());
			Node userProfile = securityModel.sync(adminSession,
					user.getUsername(), null);
			if (user instanceof NewUserDetails)
				((NewUserDetails) user).mapToProfileNode(userProfile);
			userProfile.getSession().save();

			// check in node
			VersionManager versionManager = userProfile.getSession()
					.getWorkspace().getVersionManager();
			if (versionManager.isCheckedOut(userProfile.getPath()))
				versionManager.checkin(userProfile.getPath());
			// }
			updateUser(user);
		} catch (RepositoryException e) {
			throw new ArgeoException("Cannot create user " + user, e);
		}
	}

	@Override
	public void updateUser(UserDetails userDetails) {
		try {
			User user = (User) getUserManager().getAuthorizable(
					userDetails.getUsername());
			if (user == null)
				throw new ArgeoException("No user " + userDetails.getUsername());

			// new password
			String newPassword = userDetails.getPassword();
			if (!newPassword.trim().equals("")) {
				SimpleCredentials sp = new SimpleCredentials(
						userDetails.getUsername(), newPassword.toCharArray());
				CryptedSimpleCredentials credentials = (CryptedSimpleCredentials) user
						.getCredentials();
				if (!credentials.matches(sp))
					user.changePassword(new String(newPassword));
			}

			List<String> roles = new ArrayList<String>();
			for (GrantedAuthority ga : userDetails.getAuthorities()) {
				if (ga.getAuthority().equals(userRole))
					continue;
				roles.add(ga.getAuthority());
			}

			for (Iterator<Group> it = user.memberOf(); it.hasNext();) {
				Group group = it.next();
				if (roles.contains(group.getPrincipal().getName()))
					roles.remove(group.getPrincipal().getName());
				else
					group.removeMember(user);
			}

			// remaining (new ones)
			for (String role : roles) {
				Group group = (Group) getUserManager().getAuthorizable(role);
				if (group == null)
					throw new ArgeoException("Group " + role
							+ " does not exist,"
							+ " whereas it was granted to user " + userDetails);
				group.addMember(user);
			}
		} catch (Exception e) {
			throw new ArgeoException("Cannot update user details", e);
		}

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
		String username = authentication.getName();
		try {
			SimpleCredentials sp = new SimpleCredentials(username,
					oldPassword.toCharArray());
			User user = (User) getUserManager().getAuthorizable(username);
			CryptedSimpleCredentials credentials = (CryptedSimpleCredentials) user
					.getCredentials();
			if (credentials.matches(sp))
				user.changePassword(newPassword);
			else
				throw new BadCredentialsException("Bad credentials provided");
		} catch (Exception e) {
			throw new ArgeoException("Cannot change password for user "
					+ username, e);
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
					"rep:principalName", null, UserManager.SEARCH_TYPE_USER);
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
					"rep:principalName", null, UserManager.SEARCH_TYPE_GROUP);
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
			if (user == null)
				throw new UsernameNotFoundException("User " + username
						+ " cannot be found");
			return loadJcrUserDetails(adminSession, username);
		} catch (RepositoryException e) {
			throw new ArgeoException("Cannot load user " + username, e);
		}
	}

	protected JcrUserDetails loadJcrUserDetails(Session session, String username)
			throws RepositoryException {
		if (username == null)
			username = session.getUserID();
		User user = (User) getUserManager().getAuthorizable(username);
		ArrayList<GrantedAuthorityPrincipal> authorities = new ArrayList<GrantedAuthorityPrincipal>();
		// FIXME make it more generic
		authorities.add(new GrantedAuthorityPrincipal("ROLE_USER"));
		Iterator<Group> groups = user.declaredMemberOf();
		while (groups.hasNext()) {
			Group group = groups.next();
			// String role = "ROLE_"
			// + group.getPrincipal().getName().toUpperCase();
			String role = group.getPrincipal().getName();
			authorities.add(new GrantedAuthorityPrincipal(role));
		}

		Node userProfile = UserJcrUtils.getUserProfile(session, username);
		JcrUserDetails userDetails = new JcrUserDetails(userProfile, "",
				authorities);
		return userDetails;
	}

	// AUTHENTICATION PROVIDER
	public synchronized Authentication authenticate(
			Authentication authentication) throws AuthenticationException {
		NodeAuthenticationToken siteAuth = (NodeAuthenticationToken) authentication;
		String username = siteAuth.getName();
		if (!(siteAuth.getCredentials() instanceof char[]))
			throw new ArgeoException("Only char array passwords are supported");
		char[] password = (char[]) siteAuth.getCredentials();
		try {
			SimpleCredentials sp = new SimpleCredentials(siteAuth.getName(),
					password);
			User user = (User) getUserManager().getAuthorizable(username);
			if (user == null)
				throw new BadCredentialsException("Bad credentials");
			CryptedSimpleCredentials credentials = (CryptedSimpleCredentials) user
					.getCredentials();
			// String providedPassword = siteAuth.getCredentials().toString();
			if (!credentials.matches(sp))
				throw new BadCredentialsException("Bad credentials");

			// session = repository.login(sp, null);

			Node userProfile = UserJcrUtils.getUserProfile(adminSession,
					username);
			JcrUserDetails.checkAccountStatus(userProfile);
		} catch (BadCredentialsException e) {
			throw e;
		} catch (Exception e) {
			throw new BadCredentialsException(
					"Cannot authenticate " + siteAuth, e);
		} finally {
			Arrays.fill(password, '*');
		}

		try {
			JcrUserDetails userDetails = loadJcrUserDetails(adminSession,
					username);
			NodeAuthenticationToken authenticated = new NodeAuthenticationToken(
					siteAuth, userDetails.getAuthorities());
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
