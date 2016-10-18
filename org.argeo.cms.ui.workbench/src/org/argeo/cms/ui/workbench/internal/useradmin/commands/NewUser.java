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
package org.argeo.cms.ui.workbench.internal.useradmin.commands;

import java.util.Dictionary;
import java.util.List;
import java.util.Map;

import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;

import org.argeo.cms.CmsException;
import org.argeo.cms.ui.workbench.WorkbenchUiPlugin;
import org.argeo.cms.ui.workbench.internal.useradmin.UserAdminWrapper;
import org.argeo.cms.util.useradmin.UserAdminUtils;
import org.argeo.eclipse.ui.EclipseUiUtils;
import org.argeo.eclipse.ui.dialogs.ErrorFeedback;
import org.argeo.naming.LdapAttrs;
import org.argeo.node.ArgeoNames;
import org.argeo.osgi.useradmin.UserAdminConf;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.handlers.HandlerUtil;
import org.osgi.service.useradmin.Role;
import org.osgi.service.useradmin.User;
import org.osgi.service.useradmin.UserAdminEvent;

/** Open a wizard that enables creation of a new user. */
public class NewUser extends AbstractHandler {
	/**
	 * Email addresses must match this regexp pattern ({@value #EMAIL_PATTERN}.
	 * Thanks to <a href=
	 * "http://www.mkyong.com/regular-expressions/how-to-validate-email-address-with-regular-expression/"
	 * >this tip</a>.
	 */
	public final static String EMAIL_PATTERN = "^[_A-Za-z0-9-]+(\\.[_A-Za-z0-9-]+)*@[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$";
	// private final static Log log = LogFactory.getLog(NewUser.class);
	public final static String ID = WorkbenchUiPlugin.PLUGIN_ID + ".newUser";

	/* DEPENDENCY INJECTION */
	private UserAdminWrapper userAdminWrapper;

	public Object execute(ExecutionEvent event) throws ExecutionException {
		NewUserWizard newUserWizard = new NewUserWizard();
		newUserWizard.setWindowTitle("User creation");
		WizardDialog dialog = new WizardDialog(
				HandlerUtil.getActiveShell(event), newUserWizard);
		dialog.open();
		return null;
	}

	private class NewUserWizard extends Wizard {

		// pages
		private MainUserInfoWizardPage mainUserInfo;

		// End user fields
		private Text dNameTxt, usernameTxt, firstNameTxt, lastNameTxt,
				primaryMailTxt, pwd1Txt, pwd2Txt;
		private Combo baseDnCmb;

		public NewUserWizard() {

		}

		@Override
		public void addPages() {
			mainUserInfo = new MainUserInfoWizardPage();
			addPage(mainUserInfo);
			String message = "Default wizard that also eases user creation tests:\n "
					+ "Mail and last name are automatically "
					+ "generated form the uid. Password are defauted to 'demo'.";
			mainUserInfo.setMessage(message, WizardPage.WARNING);
		}

		@SuppressWarnings({ "rawtypes", "unchecked" })
		@Override
		public boolean performFinish() {
			if (!canFinish())
				return false;
			String username = mainUserInfo.getUsername();
			userAdminWrapper.beginTransactionIfNeeded();
			try {
				User user = (User) userAdminWrapper.getUserAdmin().createRole(
						getDn(username), Role.USER);

				Dictionary props = user.getProperties();

				String lastNameStr = lastNameTxt.getText();
				if (EclipseUiUtils.notEmpty(lastNameStr))
					props.put(LdapAttrs.sn.name(), lastNameStr);

				String firstNameStr = firstNameTxt.getText();
				if (EclipseUiUtils.notEmpty(firstNameStr))
					props.put(LdapAttrs.givenName.name(), firstNameStr);

				String cn = UserAdminUtils.buildDefaultCn(firstNameStr,
						lastNameStr);
				if (EclipseUiUtils.notEmpty(cn))
					props.put(LdapAttrs.cn.name(), cn);

				String mailStr = primaryMailTxt.getText();
				if (EclipseUiUtils.notEmpty(mailStr))
					props.put(LdapAttrs.mail.name(), mailStr);

				char[] password = mainUserInfo.getPassword();
				user.getCredentials().put(null, password);
				userAdminWrapper.commitOrNotifyTransactionStateChange();
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

				baseDnCmb = createGridLC(composite, "Base DN");
				initialiseDnCmb(baseDnCmb);
				baseDnCmb.addModifyListener(this);
				baseDnCmb.addModifyListener(new ModifyListener() {
					private static final long serialVersionUID = -1435351236582736843L;

					@Override
					public void modifyText(ModifyEvent event) {
						String name = usernameTxt.getText();
						dNameTxt.setText(getDn(name));
					}
				});

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
							primaryMailTxt.setText(getMail(name));
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
				if (!primaryMailTxt.getText().matches(EMAIL_PATTERN))
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
					if (baseDnCmb.getSelectionIndex() == -1)
						baseDnCmb.setFocus();
					else
						usernameTxt.setFocus();
			}

			public String getUsername() {
				return usernameTxt.getText();
			}

			public char[] getPassword() {
				return pwd1Txt.getTextChars();
			}

		}

		private Map<String, String> getDns() {
			return userAdminWrapper.getKnownBaseDns(true);
		}

		private String getDn(String uid) {
			Map<String, String> dns = getDns();
			String bdn = baseDnCmb.getText();
			if (EclipseUiUtils.notEmpty(bdn)) {
				Dictionary<String, ?> props = UserAdminConf.uriAsProperties(dns
						.get(bdn));
				String dn = LdapAttrs.uid.name() + "=" + uid + ","
						+ UserAdminConf.userBase.getValue(props) + "," + bdn;
				return dn;
			}
			return null;
		}

		private void initialiseDnCmb(Combo combo) {
			Map<String, String> dns = userAdminWrapper.getKnownBaseDns(true);
			if (dns.isEmpty())
				throw new CmsException(
						"No writable base dn found. Cannot create user");
			combo.setItems(dns.keySet().toArray(new String[0]));
			if (dns.size() == 1)
				combo.select(0);
		}

		private String getMail(String username) {
			if (baseDnCmb.getSelectionIndex() == -1)
				return null;
			String baseDn = baseDnCmb.getText();
			try {
				LdapName name = new LdapName(baseDn);
				List<Rdn> rdns = name.getRdns();
				return username + "@" + (String) rdns.get(1).getValue() + '.'
						+ (String) rdns.get(0).getValue();
			} catch (InvalidNameException e) {
				throw new CmsException("Unable to generate mail for "
						+ username + " with base dn " + baseDn, e);
			}
		}
	}

	private Combo createGridLC(Composite parent, String label) {
		Label lbl = new Label(parent, SWT.LEAD);
		lbl.setText(label);
		lbl.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		Combo combo = new Combo(parent, SWT.LEAD | SWT.BORDER | SWT.READ_ONLY);
		combo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		return combo;
	}

	/* DEPENDENCY INJECTION */
	public void setUserAdminWrapper(UserAdminWrapper userAdminWrapper) {
		this.userAdminWrapper = userAdminWrapper;
	}
}
