package org.argeo.jcr.security;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.version.VersionManager;

import org.argeo.ArgeoException;
import org.argeo.jcr.ArgeoJcrConstants;
import org.argeo.jcr.ArgeoNames;
import org.argeo.jcr.ArgeoTypes;
import org.argeo.jcr.JcrUtils;
import org.argeo.jcr.UserJcrUtils;

/** Utilities related to Argeo security model in JCR */
public class SecurityJcrUtils implements ArgeoJcrConstants {
	/**
	 * Creates an Argeo user home, does nothing if it already exists. Session is
	 * NOT saved.
	 */
	public static Node createUserHomeIfNeeded(Session session, String username) {
		try {
			String homePath = generateUserHomePath(username);
			if (session.itemExists(homePath))
				return session.getNode(homePath);
			else {
				Node userHome = JcrUtils.mkdirs(session, homePath);
				userHome.addMixin(ArgeoTypes.ARGEO_USER_HOME);
				userHome.setProperty(ArgeoNames.ARGEO_USER_ID, username);
				
				//JcrUtils.addPrivilege(session, homePath, username, "jcr:all");
				return userHome;
			}
		} catch (RepositoryException e) {
			JcrUtils.discardQuietly(session);
			throw new ArgeoException("Cannot create home for " + username
					+ " in workspace " + session.getWorkspace().getName(), e);
		}
	}

	private static String generateUserHomePath(String username) {
		String homeBasePath = UserJcrUtils.DEFAULT_HOME_BASE_PATH;
		return homeBasePath + '/' + JcrUtils.firstCharsToPath(username, 2)
				+ '/' + username;
	}

	/**
	 * Creates a user profile in the home of this user. Creates the home if
	 * needed, but throw an exception if a profile already exists. The session
	 * is not saved and the node is in a checkedOut state (that is, it requires
	 * a subsequent checkin after saving the session).
	 */
	public static Node createUserProfile(Session session, String username) {
		try {
			Node userHome = createUserHomeIfNeeded(session, username);
			if (userHome.hasNode(ArgeoNames.ARGEO_PROFILE))
				throw new ArgeoException(
						"There is already a user profile under " + userHome);
			Node userProfile = userHome.addNode(ArgeoNames.ARGEO_PROFILE);
			userProfile.addMixin(ArgeoTypes.ARGEO_USER_PROFILE);
			userProfile.setProperty(ArgeoNames.ARGEO_USER_ID, username);
			userProfile.setProperty(ArgeoNames.ARGEO_ENABLED, true);
			userProfile.setProperty(ArgeoNames.ARGEO_ACCOUNT_NON_EXPIRED, true);
			userProfile.setProperty(ArgeoNames.ARGEO_ACCOUNT_NON_LOCKED, true);
			userProfile.setProperty(ArgeoNames.ARGEO_CREDENTIALS_NON_EXPIRED,
					true);
			return userProfile;
		} catch (RepositoryException e) {
			JcrUtils.discardQuietly(session);
			throw new ArgeoException("Cannot create user profile for "
					+ username + " in workspace "
					+ session.getWorkspace().getName(), e);
		}
	}

	/**
	 * Create user profile if needed, the session IS saved.
	 * 
	 * @return the user profile
	 */
	public static Node createUserProfileIfNeeded(Session securitySession,
			String username) {
		try {
			Node userHome = createUserHomeIfNeeded(securitySession, username);
			Node userProfile = userHome.hasNode(ArgeoNames.ARGEO_PROFILE) ? userHome
					.getNode(ArgeoNames.ARGEO_PROFILE) : createUserProfile(
					securitySession, username);
			if (securitySession.hasPendingChanges())
				securitySession.save();
			VersionManager versionManager = securitySession.getWorkspace()
					.getVersionManager();
			if (versionManager.isCheckedOut(userProfile.getPath()))
				versionManager.checkin(userProfile.getPath());
			return userProfile;
		} catch (RepositoryException e) {
			JcrUtils.discardQuietly(securitySession);
			throw new ArgeoException("Cannot create user profile for "
					+ username + " in workspace "
					+ securitySession.getWorkspace().getName(), e);
		}
	}

	/**
	 * @return null if not found *
	 */
	public static Node getUserProfile(Session session, String username) {
		try {
			Node userHome = UserJcrUtils.getUserHome(session, username);
			if (userHome == null)
				return null;
			if (userHome.hasNode(ArgeoNames.ARGEO_PROFILE))
				return userHome.getNode(ArgeoNames.ARGEO_PROFILE);
			else
				return null;
		} catch (RepositoryException e) {
			throw new ArgeoException(
					"Cannot find profile for user " + username, e);
		}
	}

	private SecurityJcrUtils() {
	}
}
