package org.argeo.security.ui.admin.wizards;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.argeo.eclipse.ui.dialogs.Error;
import org.argeo.jcr.ArgeoNames;
import org.argeo.jcr.JcrUtils;
import org.argeo.security.UserAdminService;
import org.argeo.security.jcr.JcrUserDetails;
import org.eclipse.jface.wizard.Wizard;
import org.springframework.security.GrantedAuthority;

/** Wizard to create a new user */
public class NewUserWizard extends Wizard {
	private final static Log log = LogFactory.getLog(NewUserWizard.class);

	private String homeBasePath = "/home";
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
			session.save();
			Node userHome = JcrUtils.createUserHome(session, homeBasePath,
					username);
			Node userProfile = userHome.getNode(ArgeoNames.ARGEO_PROFILE);
			mainUserInfo.mapToProfileNode(userProfile);
			String password = mainUserInfo.getPassword();
			JcrUserDetails jcrUserDetails = new JcrUserDetails(
					userHome.getPath(), username, password, true, true, true,
					true, new GrantedAuthority[0]);
			session.save();
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
			Error.show("Cannot create new user " + username, e);
			return false;
		}
	}

	public void setSession(Session session) {
		this.session = session;
	}

}
