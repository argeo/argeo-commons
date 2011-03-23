package org.argeo.security.ui.admin.wizards;

import org.argeo.eclipse.ui.EclipseUiUtils;
import org.argeo.security.UserAdminService;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;

public class MainUserInfoWizardPage extends WizardPage implements
		ModifyListener {
	private Text username, firstName, lastName, primaryEmail;

	public MainUserInfoWizardPage() {
		super("Main");
		setTitle("Required Information");
	}

	@Override
	public void createControl(Composite parent) {
		parent.setLayout(new FillLayout());
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout(2, false));
		username = EclipseUiUtils.createGridLT(composite, "Username", this);
		primaryEmail = EclipseUiUtils.createGridLT(composite, "Email", this);
		firstName = EclipseUiUtils.createGridLT(composite, "First name", this);
		lastName = EclipseUiUtils.createGridLT(composite, "Last name", this);
		setControl(composite);
	}

	@Override
	public void modifyText(ModifyEvent event) {
		String message = checkComplete();
		if (message != null)
			setMessage(message, WizardPage.ERROR);
		else {
			setMessage("Complete", WizardPage.INFORMATION);
			setPageComplete(true);
		}
	}

	/** @return error message or null if complete */
	protected String checkComplete() {
		if (!username.getText().matches(UserAdminService.USERNAME_PATTERN))
			return "Wrong user name format, should be lower case, between 3 and 15 characters with only '_' as acceptable special character.";
		if (!primaryEmail.getText().matches(UserAdminService.EMAIL_PATTERN))
			return "Not a valid email address";
		if (firstName.getText().trim().equals(""))
			return "Specify a first name";
		if (lastName.getText().trim().equals(""))
			return "Specify a last name";
		return null;
	}

	@Override
	public boolean canFlipToNextPage() {
		// TODO Auto-generated method stub
		return super.canFlipToNextPage();
	}

	@Override
	public boolean isPageComplete() {
		// TODO Auto-generated method stub
		return super.isPageComplete();
	}

}
