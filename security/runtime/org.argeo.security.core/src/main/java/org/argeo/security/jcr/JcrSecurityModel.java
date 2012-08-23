package org.argeo.security.jcr;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.security.Privilege;
import javax.jcr.version.VersionManager;

import org.argeo.ArgeoException;
import org.argeo.jcr.ArgeoJcrConstants;
import org.argeo.jcr.ArgeoNames;
import org.argeo.jcr.ArgeoTypes;
import org.argeo.jcr.JcrUtils;
import org.argeo.jcr.UserJcrUtils;

/**
 * Manages data expected by the Argeo security model, such as user home and
 * profile.
 */
public class JcrSecurityModel {
	// ArgeoNames not implemented as interface in order to ease derivation by
	// Jackrabbit bundles

	/** The home base path. */
	private String homeBasePath = "/home";

	/**
	 * To be called before user details are loaded
	 * 
	 * @return the user profile (whose parent is the user home)
	 */
	public Node sync(Session session, String username) {
		// TODO check user name validity (e.g. should not start by ROLE_)

		try {
			Node userHome = UserJcrUtils.getUserHome(session, username);
			if (userHome == null) {
				String homePath = generateUserPath(homeBasePath, username);
				userHome = JcrUtils.mkdirs(session, homePath);
				// userHome = JcrUtils.mkfolders(session, homePath);
				userHome.addMixin(ArgeoTypes.ARGEO_USER_HOME);
				userHome.setProperty(ArgeoNames.ARGEO_USER_ID, username);
				session.save();

				JcrUtils.clearAccesControList(session, homePath, username);
				JcrUtils.addPrivilege(session, homePath, username,
						Privilege.JCR_ALL);
			}

			Node userProfile = UserJcrUtils.getUserProfile(session, username);
			if (userProfile == null) {
				String personPath = generateUserPath(
						ArgeoJcrConstants.PEOPLE_BASE_PATH, username);
				Node personBase = JcrUtils.mkdirs(session, personPath);
				userProfile = personBase.addNode(ArgeoNames.ARGEO_PROFILE);
				userProfile.addMixin(ArgeoTypes.ARGEO_USER_PROFILE);
				userProfile.setProperty(ArgeoNames.ARGEO_USER_ID, username);
				userProfile.setProperty(ArgeoNames.ARGEO_ENABLED, true);
				userProfile.setProperty(ArgeoNames.ARGEO_ACCOUNT_NON_EXPIRED,
						true);
				userProfile.setProperty(ArgeoNames.ARGEO_ACCOUNT_NON_LOCKED,
						true);
				userProfile.setProperty(
						ArgeoNames.ARGEO_CREDENTIALS_NON_EXPIRED, true);
				session.save();

				JcrUtils.clearAccesControList(session, userProfile.getPath(),
						username);
				JcrUtils.addPrivilege(session, userProfile.getPath(), username,
						Privilege.JCR_READ);

				VersionManager versionManager = session.getWorkspace()
						.getVersionManager();
				if (versionManager.isCheckedOut(userProfile.getPath()))
					versionManager.checkin(userProfile.getPath());
			}
			return userProfile;
		} catch (RepositoryException e) {
			JcrUtils.discardQuietly(session);
			throw new ArgeoException("Cannot sync node security model for "
					+ username, e);
		}
	}

	/** Generate path for a new user home */
	protected String generateUserPath(String base, String username) {
		int atIndex = username.indexOf('@');
		if (atIndex > 0) {
			String domain = username.substring(0, atIndex);
			String name = username.substring(atIndex + 1);
			return base + '/' + JcrUtils.firstCharsToPath(domain, 2) + '/'
					+ domain + '/' + JcrUtils.firstCharsToPath(name, 2) + '/'
					+ name;
		} else if (atIndex == 0 || atIndex == (username.length() - 1)) {
			throw new ArgeoException("Unsupported username " + username);
		} else {
			return base + '/' + JcrUtils.firstCharsToPath(username, 2) + '/'
					+ username;
		}
	}

	public void setHomeBasePath(String homeBasePath) {
		this.homeBasePath = homeBasePath;
	}

}
