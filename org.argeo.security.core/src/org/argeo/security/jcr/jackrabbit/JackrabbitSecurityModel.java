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
package org.argeo.security.jcr.jackrabbit;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.argeo.ArgeoException;
import org.argeo.jcr.ArgeoNames;
import org.argeo.security.jcr.SimpleJcrSecurityModel;

/** Make sure that user authorizable exists before syncing user directories. */
public class JackrabbitSecurityModel extends SimpleJcrSecurityModel {
	private final static Log log = LogFactory
			.getLog(JackrabbitSecurityModel.class);

	@Override
	public synchronized Node sync(Session session, String username,
			List<String> roles) {
		if (!(session instanceof JackrabbitSession))
			return super.sync(session, username, roles);

		try {
			UserManager userManager = ((JackrabbitSession) session)
					.getUserManager();
			User user = (User) userManager.getAuthorizable(username);
			if (user != null) {
				String principalName = user.getPrincipal().getName();
				if (!principalName.equals(username)) {
					log.warn("Jackrabbit principal is '" + principalName
							+ "' but username is '" + username
							+ "'. Recreating...");
					user.remove();
					user = userManager.createUser(username, "");
				}
			} else {
				// create new principal
				user = userManager.createUser(username, "");
				log.info(username + " added as Jackrabbit user " + user);
			}

			// generic JCR sync
			Node userProfile = super.sync(session, username, roles);

			Boolean enabled = userProfile.getProperty(ArgeoNames.ARGEO_ENABLED)
					.getBoolean();
			if (enabled && user.isDisabled())
				user.disable(null);
			else if (!enabled && !user.isDisabled())
				user.disable(userProfile.getPath() + " is disabled");

			// Sync Jackrabbit roles
			if (roles != null)
				syncRoles(userManager, user, roles);

			return userProfile;
		} catch (RepositoryException e) {
			throw new ArgeoException(
					"Cannot perform Jackrabbit specific operations", e);
		}
	}

	/** Make sure Jackrabbit roles are in line with authentication */
	void syncRoles(UserManager userManager, User user, List<String> roles)
			throws RepositoryException {
		List<String> userGroupIds = new ArrayList<String>();
		for (String role : roles) {
			Group group = (Group) userManager.getAuthorizable(role);
			if (group == null) {
				group = userManager.createGroup(role);
				log.info(role + " added as " + group);
			}
			if (!group.isMember(user))
				group.addMember(user);
			userGroupIds.add(role);
		}

		// check if user has not been removed from some groups
		for (Iterator<Group> it = user.declaredMemberOf(); it.hasNext();) {
			Group group = it.next();
			if (!userGroupIds.contains(group.getID()))
				group.removeMember(user);
		}
	}
}
