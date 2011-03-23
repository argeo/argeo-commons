package org.argeo.security.ui.admin.wizards;

import javax.jcr.Node;
import javax.jcr.Session;

import org.argeo.eclipse.ui.dialogs.Error;
import org.argeo.jcr.ArgeoNames;
import org.argeo.jcr.JcrUtils;
import org.argeo.security.UserAdminService;
import org.argeo.security.jcr.JcrUserDetails;
import org.eclipse.jface.wizard.Wizard;
import org.springframework.security.GrantedAuthority;

/** Wizard to create a new user */
public class NewUserWizard extends Wizard {
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

		try {
			String username = mainUserInfo.getUsername();
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
			Error.show("Cannot create new user", e);
			return false;
		}
	}

	public void setSession(Session session) {
		this.session = session;
	}

}
