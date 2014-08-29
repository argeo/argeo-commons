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
package org.argeo.security.ui.admin.wizards;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;

import org.argeo.ArgeoException;
import org.argeo.eclipse.ui.EclipseUiUtils;
import org.argeo.jcr.ArgeoNames;
import org.argeo.security.UserAdminService;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.springframework.security.userdetails.UserDetails;
import org.springframework.security.userdetails.UsernameNotFoundException;

public class MainUserInfoWizardPage extends WizardPage implements
		ModifyListener, ArgeoNames {
	private Text username, firstName, lastName, primaryEmail, password1,
			password2;
	private UserAdminService userAdminService;

	public MainUserInfoWizardPage(UserAdminService userAdminService) {
		super("Main");
		this.userAdminService = userAdminService;
		setTitle("Required Information");
	}

	@Override
	public void createControl(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout(2, false));
		username = EclipseUiUtils.createGridLT(composite, "Username", this);
		primaryEmail = EclipseUiUtils.createGridLT(composite, "Email", this);
		firstName = EclipseUiUtils.createGridLT(composite, "First name", this);
		lastName = EclipseUiUtils.createGridLT(composite, "Last name", this);
		password1 = EclipseUiUtils.createGridLP(composite, "Password", this);
		password2 = EclipseUiUtils.createGridLP(composite, "Repeat password",
				this);
		setControl(composite);
		
		// Initialize buttons
		setPageComplete(false);
		getContainer().updateButtons();
	}

	@Override
	public void modifyText(ModifyEvent event) {
		String message = checkComplete();
		if (message != null) {
			setMessage(message, WizardPage.ERROR);
			setPageComplete(false);
		} else {
			setMessage("Complete", WizardPage.INFORMATION);
			setPageComplete(true);
		}
		getContainer().updateButtons();
	}

	/** @return error message or null if complete */
	protected String checkComplete() {
		// if (!username.getText().matches(UserAdminService.USERNAME_PATTERN))
		// return
		// "Wrong user name format, should be lower case, between 3 and 64 characters with only '_' an '@' as acceptable special character.";
		
		if (username.getText().trim().equals(""))
			return "User name must not be empty";
		
		try {
			UserDetails userDetails = userAdminService
					.loadUserByUsername(username.getText());
			return "User " + userDetails.getUsername() + " already exists";
		} catch (UsernameNotFoundException e) {
			// silent
		}
		if (!primaryEmail.getText().matches(UserAdminService.EMAIL_PATTERN))
			return "Not a valid email address";
		if (firstName.getText().trim().equals(""))
			return "Specify a first name";
		if (lastName.getText().trim().equals(""))
			return "Specify a last name";
		if (password1.getText().trim().equals(""))
			return "Specify a password";
		if (password2.getText().trim().equals(""))
			return "Repeat the password";
		if (!password2.getText().equals(password1.getText()))
			return "Passwords are different";
		return null;
	}

	public String getUsername() {
		return username.getText();
	}

	public String getPassword() {
		return password1.getText();
	}

	public void mapToProfileNode(Node up) {
		try {
			up.setProperty(ARGEO_PRIMARY_EMAIL, primaryEmail.getText());
			up.setProperty(ARGEO_FIRST_NAME, firstName.getText());
			up.setProperty(ARGEO_LAST_NAME, lastName.getText());

			// derived values
			// TODO add wizard pages to do it
			up.setProperty(Property.JCR_TITLE, firstName.getText() + " "
					+ lastName.getText());
			up.setProperty(Property.JCR_DESCRIPTION, "");
		} catch (RepositoryException e) {
			throw new ArgeoException("Cannot map to " + up, e);
		}
	}
}
