package org.argeo.security.ui.admin.wizards;

import org.eclipse.jface.wizard.Wizard;

public class NewUserWizard extends Wizard {
	private MainUserInfoWizardPage mainUserInfo;

	@Override
	public void addPages() {
		mainUserInfo = new MainUserInfoWizardPage();
		addPage(mainUserInfo);
	}

	@Override
	public boolean performFinish() {
		return false;
	}

}
