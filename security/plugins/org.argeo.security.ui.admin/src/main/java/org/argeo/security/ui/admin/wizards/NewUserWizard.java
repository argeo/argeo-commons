package org.argeo.security.ui.admin.wizards;

import javax.jcr.Node;
import javax.jcr.Session;

import org.argeo.eclipse.ui.dialogs.Error;
import org.argeo.jcr.ArgeoNames;
import org.argeo.jcr.JcrUtils;
import org.argeo.security.jcr.JcrUserDetails;
import org.eclipse.jface.wizard.Wizard;
import org.springframework.security.GrantedAuthority;
import org.springframework.security.userdetails.UserDetailsManager;

/** Wizard to create a new user */
public class NewUserWizard extends Wizard {
	private String homeBasePath = "/home";
	private Session session;
	private UserDetailsManager userDetailsManager;

	// pages
	private MainUserInfoWizardPage mainUserInfo;

	public NewUserWizard(Session session, UserDetailsManager userDetailsManager) {
		this.session = session;
		this.userDetailsManager = userDetailsManager;
	}

	@Override
	public void addPages() {
		mainUserInfo = new MainUserInfoWizardPage();
		addPage(mainUserInfo);
	}

	@Override
	public boolean performFinish() {
		try {
			String username = mainUserInfo.getUsername();
			Node userHome = JcrUtils.createUserHome(session, homeBasePath,
					username);
			Node userProfile = userHome.getNode(ArgeoNames.ARGEO_PROFILE);
			mainUserInfo.mapToProfileNode(userProfile);
			String password = mainUserInfo.getPassword();
			JcrUserDetails jcrUserDetails = new JcrUserDetails(
					userHome.getPath(), username, password, true, true, true,
					true, new GrantedAuthority[0]);
			session.save();
			userDetailsManager.createUser(jcrUserDetails);
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
