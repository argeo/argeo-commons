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
package org.argeo.security.ui.admin.internal.commands;

import java.util.Dictionary;

import org.argeo.eclipse.ui.EclipseUiUtils;
import org.argeo.eclipse.ui.dialogs.ErrorFeedback;
import org.argeo.jcr.ArgeoNames;
import org.argeo.security.UserAdminService;
import org.argeo.security.ui.admin.SecurityAdminPlugin;
import org.argeo.security.ui.admin.internal.UiAdminUtils;
import org.argeo.security.ui.admin.internal.UserAdminConstants;
import org.argeo.security.ui.admin.internal.UserAdminWrapper;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.handlers.HandlerUtil;
import org.osgi.service.useradmin.Role;
import org.osgi.service.useradmin.User;
import org.osgi.service.useradmin.UserAdminEvent;

/** Open a wizard that enables creation of a new user. */
public class NewUser extends AbstractHandler {
	// private final static Log log = LogFactory.getLog(NewUser.class);
	public final static String ID = SecurityAdminPlugin.PLUGIN_ID + ".newUser";

	/* DEPENDENCY INJECTION */
	private UserAdminWrapper userAdminWrapper;

	// TODO implement a dynamic choice of the base dn
	private String getDn(String uid) {
		return "uid=" + uid + ",ou=users,dc=example,dc=com";
	}

	public Object execute(ExecutionEvent event) throws ExecutionException {
		NewUserWizard newUserWizard = new NewUserWizard();
		WizardDialog dialog = new WizardDialog(
				HandlerUtil.getActiveShell(event), newUserWizard);

		dialog.open();

		// // Force refresh until the listener are implemented
		// if (Window.OK == dialog.open())
		// forceRefresh(event);
		return null;
	}

	// private void forceRefresh(ExecutionEvent event) {
	// IWorkbenchWindow iww = HandlerUtil.getActiveWorkbenchWindow(event);
	// if (iww == null)
	// return;
	// IWorkbenchPage activePage = iww.getActivePage();
	// IWorkbenchPart part = activePage.getActivePart();
	// if (part instanceof UsersView)
	// ((UsersView) part).refresh();
	// }

	private class NewUserWizard extends Wizard {

		// pages
		private MainUserInfoWizardPage mainUserInfo;

		// End user fields
		private Text dNameTxt, usernameTxt, firstNameTxt, lastNameTxt,
				primaryMailTxt, pwd1Txt, pwd2Txt;

		public NewUserWizard() {
		}

		@Override
		public void addPages() {
			mainUserInfo = new MainUserInfoWizardPage();
			addPage(mainUserInfo);
			String message = "Dummy wizard to ease user creation tests:\n Mail and last name are automatically "
					+ "generated form the uid. Password are defauted to 'demo'.";
			mainUserInfo.setMessage(message, WizardPage.WARNING);
		}

		@SuppressWarnings({ "rawtypes", "unchecked" })
		@Override
		public boolean performFinish() {
			if (!canFinish())
				return false;
			String username = mainUserInfo.getUsername();
			try {
				userAdminWrapper.beginTransactionIfNeeded();
				User user = (User) userAdminWrapper.getUserAdmin().createRole(
						getDn(username), Role.USER);

				Dictionary props = user.getProperties();

				String lastNameStr = lastNameTxt.getText();
				if (UiAdminUtils.notNull(lastNameStr))
					props.put(UserAdminConstants.KEY_LASTNAME, lastNameStr);

				String firstNameStr = firstNameTxt.getText();
				if (UiAdminUtils.notNull(firstNameStr))
					props.put(UserAdminConstants.KEY_FIRSTNAME, firstNameStr);

				String cn = UiAdminUtils
						.getDefaultCn(firstNameStr, lastNameStr);
				if (UiAdminUtils.notNull(cn))
					props.put(UserAdminConstants.KEY_CN, cn);

				String mailStr = primaryMailTxt.getText();
				if (UiAdminUtils.notNull(mailStr))
					props.put(UserAdminConstants.KEY_MAIL, mailStr);

				char[] password = mainUserInfo.getPassword();
				user.getCredentials().put(null, password);

				userAdminWrapper.notifyListeners(new UserAdminEvent(null,
						UserAdminEvent.ROLE_CREATED, user));
				return true;
			} catch (Exception e) {
				ErrorFeedback.show("Cannot create new user " + username, e);
				return false;
			}
		}

		private class MainUserInfoWizardPage extends WizardPage implements
				ModifyListener, ArgeoNames {
			private static final long serialVersionUID = -3150193365151601807L;

			public MainUserInfoWizardPage() {
				super("Main");
				setTitle("Required Information");
			}

			@Override
			public void createControl(Composite parent) {
				Composite composite = new Composite(parent, SWT.NONE);
				composite.setLayout(new GridLayout(2, false));
				dNameTxt = EclipseUiUtils.createGridLT(composite,
						"Distinguished name", this);
				dNameTxt.setEnabled(false);
				usernameTxt = EclipseUiUtils.createGridLT(composite,
						"Local ID", this);
				usernameTxt.addModifyListener(new ModifyListener() {
					private static final long serialVersionUID = -1435351236582736843L;

					@Override
					public void modifyText(ModifyEvent event) {
						String name = usernameTxt.getText();
						if (name.trim().equals("")) {
							dNameTxt.setText("");
							lastNameTxt.setText("");
							primaryMailTxt.setText("");
							pwd1Txt.setText("");
							pwd2Txt.setText("");
						} else {
							dNameTxt.setText(getDn(name));
							lastNameTxt.setText(name.toUpperCase());
							primaryMailTxt.setText(name + "@example.com");
							pwd1Txt.setText("demo");
							pwd2Txt.setText("demo");
						}
					}
				});

				primaryMailTxt = EclipseUiUtils.createGridLT(composite,
						"Email", this);
				firstNameTxt = EclipseUiUtils.createGridLT(composite,
						"First name", this);
				lastNameTxt = EclipseUiUtils.createGridLT(composite,
						"Last name", this);
				pwd1Txt = EclipseUiUtils.createGridLP(composite, "Password",
						this);
				pwd2Txt = EclipseUiUtils.createGridLP(composite,
						"Repeat password", this);
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
				String name = usernameTxt.getText();

				if (name.trim().equals(""))
					return "User name must not be empty";
				Role role = userAdminWrapper.getUserAdmin()
						.getRole(getDn(name));
				if (role != null)
					return "User " + name + " already exists";
				if (!primaryMailTxt.getText().matches(
						UserAdminService.EMAIL_PATTERN))
					return "Not a valid email address";
				if (lastNameTxt.getText().trim().equals(""))
					return "Specify a last name";
				if (pwd1Txt.getText().trim().equals(""))
					return "Specify a password";
				if (pwd2Txt.getText().trim().equals(""))
					return "Repeat the password";
				if (!pwd2Txt.getText().equals(pwd1Txt.getText()))
					return "Passwords are different";
				return null;
			}

			@Override
			public void setVisible(boolean visible) {
				super.setVisible(visible);
				if (visible)
					usernameTxt.setFocus();
			}

			public String getUsername() {
				return usernameTxt.getText();
			}

			public char[] getPassword() {
				return pwd1Txt.getTextChars();
			}

		}
	}

	/* DEPENDENCY INJECTION */
	public void setUserAdminWrapper(UserAdminWrapper userAdminWrapper) {
		this.userAdminWrapper = userAdminWrapper;
	}
}