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
package org.argeo.cms.users;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.argeo.eclipse.ui.dialogs.ErrorFeedback;
import org.argeo.jcr.JcrUtils;
import org.argeo.jcr.UserJcrUtils;
import org.argeo.security.UserAdminService;
import org.argeo.security.jcr.NewUserDetails;
import org.eclipse.jface.wizard.Wizard;

/** Wizard to create a new user */
public class NewUserWizard extends Wizard {
	private final static Log log = LogFactory.getLog(NewUserWizard.class);
	private Session session;
	private UserAdminService userAdminService;
	// private JcrSecurityModel jcrSecurityModel;

	// pages
	private MainUserInfoWizardPage mainUserInfo;

	public NewUserWizard(Session session, UserAdminService userAdminService) {
		this.session = session;
		this.userAdminService = userAdminService;
		// this.jcrSecurityModel = jcrSecurityModel;
	}

	@Override
	public void addPages() {
		mainUserInfo = new MainUserInfoWizardPage(userAdminService);
		addPage(mainUserInfo);
	}

	@Override
	public boolean performFinish() {
		if (!canFinish())
			return false;

		String username = mainUserInfo.getUsername();
		try {
			// Node userProfile = SecurityJcrUtils.createUserProfile(session,
			// username);
			// Node userProfile = jcrSecurityModel.sync(session, username,
			// null);
			// session.getWorkspace().getVersionManager()
			// .checkout(userProfile.getPath());
			// mainUserInfo.mapToProfileNode(userProfile);
			char[] password = mainUserInfo.getPassword();
			// TODO add roles
			NewUserDetails jcrUserDetails = new NewUserDetails(username,
					password) {
				private static final long serialVersionUID = 7480071525603380742L;

				@Override
				public void mapToProfileNode(Node userProfile)
						throws RepositoryException {
					mainUserInfo.mapToProfileNode(userProfile);
				}
			};
			// session.save();
			// session.getWorkspace().getVersionManager()
			// .checkin(userProfile.getPath());
			userAdminService.createUser(jcrUserDetails);
			return true;
		} catch (Exception e) {
			JcrUtils.discardQuietly(session);
			Node userHome = UserJcrUtils.getUserHome(session, username);
			if (userHome != null) {
				try {
					userHome.remove();
					session.save();
				} catch (RepositoryException e1) {
					JcrUtils.discardQuietly(session);
					log.warn("Error when trying to clean up failed new user "
							+ username, e1);
				}
			}
			ErrorFeedback.show("Cannot create new user " + username, e);
			return false;
		}
	}

	public void setSession(Session session) {
		this.session = session;
	}

}
