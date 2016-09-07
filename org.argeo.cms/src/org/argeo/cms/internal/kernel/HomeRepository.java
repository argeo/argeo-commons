package org.argeo.cms.internal.kernel;

import java.security.PrivilegedAction;
import java.util.HashSet;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.security.Privilege;
import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.security.auth.Subject;
import javax.security.auth.login.LoginContext;

import org.apache.jackrabbit.core.security.SecurityConstants;
import org.argeo.ArgeoException;
import org.argeo.cms.CmsException;
import org.argeo.cms.auth.AuthConstants;
import org.argeo.jcr.ArgeoJcrConstants;
import org.argeo.jcr.ArgeoNames;
import org.argeo.jcr.ArgeoTypes;
import org.argeo.jcr.JcrRepositoryWrapper;
import org.argeo.jcr.JcrUtils;
import org.argeo.jcr.UserJcrUtils;

/**
 * Make sure each user has a home directory available in the default workspace.
 */
class HomeRepository extends JcrRepositoryWrapper implements KernelConstants, ArgeoJcrConstants {
	// private final Kernel kernel;

	/** The home base path. */
	private String homeBasePath = "/home";
	private String peopleBasePath = ArgeoJcrConstants.PEOPLE_BASE_PATH;

	private Set<String> checkedUsers = new HashSet<String>();

	public HomeRepository(Repository repository) {
		// this.kernel = kernel;
		setRepository(repository);
		LoginContext lc;
		try {
			lc = new LoginContext(AuthConstants.LOGIN_CONTEXT_DATA_ADMIN);
			lc.login();
		} catch (javax.security.auth.login.LoginException e1) {
			throw new CmsException("Cannot login as systrem", e1);
		}
		Subject.doAs(lc.getSubject(), new PrivilegedAction<Void>() {

			@Override
			public Void run() {
				try {
					initJcr(getRepository().login());
				} catch (RepositoryException e) {
					throw new CmsException("Cannot init JCR home", e);
				}
				return null;
			}

		});
	}

	// @Override
	// public Session login() throws LoginException, RepositoryException {
	// Session session = super.login();
	// String username = session.getUserID();
	// if (username == null)
	// return session;
	// if (session.getUserID().equals(AuthConstants.ROLE_ANONYMOUS))
	// return session;
	//
	// if (checkedUsers.contains(username))
	// return session;
	// Session adminSession = KernelUtils.openAdminSession(getRepository(),
	// session.getWorkspace().getName());
	// try {
	// syncJcr(adminSession, username);
	// checkedUsers.add(username);
	// } finally {
	// JcrUtils.logoutQuietly(adminSession);
	// }
	// return session;
	// }

	@Override
	protected void processNewSession(Session session) {
		String username = session.getUserID();
		if (username == null)
			return;
		if (session.getUserID().equals(AuthConstants.ROLE_ANONYMOUS))
			return;
		if (session.getUserID().equals(AuthConstants.ROLE_KERNEL))
			return;
		if (session.getUserID().equals(SecurityConstants.ADMIN_ID))
			return;

		if (checkedUsers.contains(username))
			return;
		Session adminSession = KernelUtils.openAdminSession(getRepository(), session.getWorkspace().getName());
		try {
			syncJcr(adminSession, username);
			checkedUsers.add(username);
		} finally {
			JcrUtils.logoutQuietly(adminSession);
		}
	}

	/*
	 * JCR
	 */
	/** Session is logged out. */
	private void initJcr(Session adminSession) {
		try {
			JcrUtils.mkdirs(adminSession, homeBasePath);
			JcrUtils.mkdirs(adminSession, peopleBasePath);
			adminSession.save();

			JcrUtils.addPrivilege(adminSession, homeBasePath, AuthConstants.ROLE_USER_ADMIN, Privilege.JCR_READ);
			JcrUtils.addPrivilege(adminSession, peopleBasePath, AuthConstants.ROLE_USER_ADMIN, Privilege.JCR_ALL);
			adminSession.save();
		} catch (RepositoryException e) {
			throw new CmsException("Cannot initialize node user admin", e);
		} finally {
			JcrUtils.logoutQuietly(adminSession);
		}
	}

	private Node syncJcr(Session session, String username) {
		try {
			Node userHome = UserJcrUtils.getUserHome(session, username);
			if (userHome == null) {
				String homePath = generateUserPath(homeBasePath, username);
				if (session.itemExists(homePath))// duplicate user id
					userHome = session.getNode(homePath).getParent().addNode(JcrUtils.lastPathElement(homePath));
				else
					userHome = JcrUtils.mkdirs(session, homePath);
				// userHome = JcrUtils.mkfolders(session, homePath);
				userHome.addMixin(ArgeoTypes.ARGEO_USER_HOME);
				userHome.setProperty(ArgeoNames.ARGEO_USER_ID, username);
				session.save();

				JcrUtils.clearAccessControList(session, homePath, username);
				JcrUtils.addPrivilege(session, homePath, username, Privilege.JCR_ALL);
			}

			Node userProfile = UserJcrUtils.getUserProfile(session, username);
			// new user
			if (userProfile == null) {
				String personPath = generateUserPath(peopleBasePath, username);
				Node personBase;
				if (session.itemExists(personPath))// duplicate user id
					personBase = session.getNode(personPath).getParent().addNode(JcrUtils.lastPathElement(personPath));
				else
					personBase = JcrUtils.mkdirs(session, personPath);
				userProfile = personBase.addNode(ArgeoNames.ARGEO_PROFILE);
				userProfile.addMixin(ArgeoTypes.ARGEO_USER_PROFILE);
				userProfile.setProperty(ArgeoNames.ARGEO_USER_ID, username);
				// userProfile.setProperty(ArgeoNames.ARGEO_ENABLED, true);
				// userProfile.setProperty(ArgeoNames.ARGEO_ACCOUNT_NON_EXPIRED,
				// true);
				// userProfile.setProperty(ArgeoNames.ARGEO_ACCOUNT_NON_LOCKED,
				// true);
				// userProfile.setProperty(ArgeoNames.ARGEO_CREDENTIALS_NON_EXPIRED,
				// true);
				session.save();

				JcrUtils.clearAccessControList(session, userProfile.getPath(), username);
				JcrUtils.addPrivilege(session, userProfile.getPath(), username, Privilege.JCR_READ);
			}

			// Remote roles
			// if (roles != null) {
			// writeRemoteRoles(userProfile, roles);
			// }
			if (session.hasPendingChanges())
				session.save();
			return userProfile;
		} catch (RepositoryException e) {
			JcrUtils.discardQuietly(session);
			throw new ArgeoException("Cannot sync node security model for " + username, e);
		}
	}

	/** Generate path for a new user home */
	private String generateUserPath(String base, String username) {
		LdapName dn;
		try {
			dn = new LdapName(username);
		} catch (InvalidNameException e) {
			throw new ArgeoException("Invalid name " + username, e);
		}
		String userId = dn.getRdn(dn.size() - 1).getValue().toString();
		int atIndex = userId.indexOf('@');
		if (atIndex > 0) {
			String domain = userId.substring(0, atIndex);
			String name = userId.substring(atIndex + 1);
			return base + '/' + JcrUtils.firstCharsToPath(domain, 2) + '/' + domain + '/'
					+ JcrUtils.firstCharsToPath(name, 2) + '/' + name;
		} else if (atIndex == 0 || atIndex == (userId.length() - 1)) {
			throw new ArgeoException("Unsupported username " + userId);
		} else {
			return base + '/' + JcrUtils.firstCharsToPath(userId, 2) + '/' + userId;
		}
	}

}
