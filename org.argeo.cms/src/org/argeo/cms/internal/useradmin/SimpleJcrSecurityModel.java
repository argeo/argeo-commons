/*
 * Copyright (C) 2007-2012 Argeo GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.argeo.cms.internal.useradmin;

import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.security.Privilege;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.argeo.ArgeoException;
import org.argeo.cms.internal.auth.JcrSecurityModel;
import org.argeo.jcr.ArgeoJcrConstants;
import org.argeo.jcr.ArgeoNames;
import org.argeo.jcr.ArgeoTypes;
import org.argeo.jcr.JcrUtils;
import org.argeo.jcr.UserJcrUtils;

/**
 * Manages data expected by the Argeo security model, such as user home and
 * profile.
 */
public class SimpleJcrSecurityModel implements JcrSecurityModel {
	private final static Log log = LogFactory
			.getLog(SimpleJcrSecurityModel.class);
	// ArgeoNames not implemented as interface in order to ease derivation by
	// Jackrabbit bundles

	/** The home base path. */
	private String homeBasePath = "/home";

	public synchronized Node sync(Session session, String username,
			List<String> roles) {
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

				JcrUtils.clearAccessControList(session, homePath, username);
				JcrUtils.addPrivilege(session, homePath, username,
						Privilege.JCR_ALL);
			} else {
				// for backward compatibility with pre 1.0 security model
				if (userHome.hasNode(ArgeoNames.ARGEO_PROFILE)) {
					userHome.getNode(ArgeoNames.ARGEO_PROFILE).remove();
					userHome.getSession().save();
				}
			}

			// Remote roles
			if (roles != null) {
				// writeRemoteRoles(userHome, roles);
			}

			Node userProfile = UserJcrUtils.getUserProfile(session, username);
			// new user
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

				JcrUtils.clearAccessControList(session, userProfile.getPath(),
						username);
				JcrUtils.addPrivilege(session, userProfile.getPath(), username,
						Privilege.JCR_READ);
			}

			// Remote roles
			if (roles != null) {
				writeRemoteRoles(userProfile, roles);
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

	/** Write remote roles used by remote access in the home directory */
	protected void writeRemoteRoles(Node userHome, List<String> roles)
			throws RepositoryException {
		boolean writeRoles = false;
		if (userHome.hasProperty(ArgeoNames.ARGEO_REMOTE_ROLES)) {
			Value[] remoteRoles = userHome.getProperty(
					ArgeoNames.ARGEO_REMOTE_ROLES).getValues();
			if (remoteRoles.length != roles.size())
				writeRoles = true;
			else
				for (int i = 0; i < remoteRoles.length; i++)
					if (!remoteRoles[i].getString().equals(roles.get(i)))
						writeRoles = true;
		} else
			writeRoles = true;

		if (writeRoles) {
			userHome.getSession().getWorkspace().getVersionManager()
					.checkout(userHome.getPath());
			String[] roleIds = roles.toArray(new String[roles.size()]);
			userHome.setProperty(ArgeoNames.ARGEO_REMOTE_ROLES, roleIds);
			JcrUtils.updateLastModified(userHome);
			userHome.getSession().save();
			userHome.getSession().getWorkspace().getVersionManager()
					.checkin(userHome.getPath());
			if (log.isDebugEnabled())
				log.debug("Wrote remote roles " + roles + " for "
						+ userHome.getProperty(ArgeoNames.ARGEO_USER_ID));
		}

	}

	public void setHomeBasePath(String homeBasePath) {
		this.homeBasePath = homeBasePath;
	}

}
