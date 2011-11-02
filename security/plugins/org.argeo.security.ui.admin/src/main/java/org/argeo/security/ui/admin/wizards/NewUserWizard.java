package org.argeo.security.ui.admin.wizards;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.argeo.eclipse.ui.ErrorFeedback;
import org.argeo.jcr.JcrUtils;
import org.argeo.security.UserAdminService;
import org.argeo.security.jcr.JcrUserDetails;
import org.eclipse.jface.wizard.Wizard;
import org.springframework.security.GrantedAuthority;

/** Wizard to create a new user */
public class NewUserWizard extends Wizard {
	private final static Log log = LogFactory.getLog(NewUserWizard.class);
	private Session session;
	private UserAdminService userAdminService;

	// pages
	private MainUserInfoWizardPage mainUserInfo;

	public NewUserWizard(Session session, UserAdminService userAdminService) {
		this.session = session;
		this.userAdminService = userAdminService;
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
			Node userProfile = JcrUtils.createUserProfile(session, username);
			session.getWorkspace().getVersionManager()
					.checkout(userProfile.getPath());
			mainUserInfo.mapToProfileNode(userProfile);
			String password = mainUserInfo.getPassword();
			// TODO add roles
			JcrUserDetails jcrUserDetails = new JcrUserDetails(userProfile,
					password, new GrantedAuthority[0]);
			session.save();
			session.getWorkspace().getVersionManager()
					.checkin(userProfile.getPath());
			userAdminService.createUser(jcrUserDetails);
			return true;
		} catch (Exception e) {
			JcrUtils.discardQuietly(session);
			Node userHome = JcrUtils.getUserHome(session, username);
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
