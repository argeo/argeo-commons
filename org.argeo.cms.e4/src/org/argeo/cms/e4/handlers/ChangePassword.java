package org.argeo.cms.e4.handlers;

import static org.argeo.cms.CmsMsg.changePassword;
import static org.argeo.cms.CmsMsg.currentPassword;
import static org.argeo.cms.CmsMsg.newPassword;
import static org.argeo.cms.CmsMsg.passwordChanged;
import static org.argeo.cms.CmsMsg.repeatNewPassword;

import java.util.Arrays;

import javax.inject.Inject;
import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;

import org.argeo.api.security.CryptoKeyring;
import org.argeo.cms.CmsException;
import org.argeo.cms.auth.CurrentUser;
import org.argeo.cms.swt.dialogs.CmsMessageDialog;
import org.argeo.eclipse.ui.dialogs.ErrorFeedback;
import org.argeo.osgi.transaction.WorkTransaction;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.osgi.service.useradmin.User;
import org.osgi.service.useradmin.UserAdmin;

/** Change the password of the logged-in user. */
public class ChangePassword {
	@Inject
	private UserAdmin userAdmin;
	@Inject
	private WorkTransaction userTransaction;
	@Inject
	@Optional
	private CryptoKeyring keyring = null;

	@Execute
	public void execute() {
		ChangePasswordDialog dialog = new ChangePasswordDialog(Display.getCurrent().getActiveShell(), userAdmin);
		if (dialog.open() == Dialog.OK) {
			new CmsMessageDialog(Display.getCurrent().getActiveShell(), passwordChanged.lead(),
					CmsMessageDialog.INFORMATION).open();
		}
	}

	protected void changePassword(char[] oldPassword, char[] newPassword) {
		String name = CurrentUser.getUsername();
		LdapName dn;
		try {
			dn = new LdapName(name);
		} catch (InvalidNameException e) {
			throw new CmsException("Invalid user dn " + name, e);
		}
		User user = (User) userAdmin.getRole(dn.toString());
		if (!user.hasCredential(null, oldPassword))
			throw new CmsException("Invalid password");
		if (Arrays.equals(newPassword, new char[0]))
			throw new CmsException("New password empty");
		try {
			userTransaction.begin();
			user.getCredentials().put(null, newPassword);
			if (keyring != null) {
				keyring.changePassword(oldPassword, newPassword);
				// TODO change secret keys in the CMS session
			}
			userTransaction.commit();
		} catch (Exception e) {
			try {
				userTransaction.rollback();
			} catch (Exception e1) {
				e1.printStackTrace();
			}
			if (e instanceof RuntimeException)
				throw (RuntimeException) e;
			else
				throw new CmsException("Cannot change password", e);
		}
	}

	class ChangePasswordDialog extends CmsMessageDialog {
		private Text oldPassword, newPassword1, newPassword2;

		public ChangePasswordDialog(Shell parentShell, UserAdmin securityService) {
			super(parentShell, changePassword.lead(), CONFIRM);
		}

//		protected Point getInitialSize() {
//			return new Point(400, 450);
//		}

		protected Control createDialogArea(Composite parent) {
			Composite dialogarea = (Composite) super.createDialogArea(parent);
			dialogarea.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
			Composite composite = new Composite(dialogarea, SWT.NONE);
			composite.setLayout(new GridLayout(2, false));
			composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			oldPassword = createLP(composite, currentPassword.lead());
			newPassword1 = createLP(composite, newPassword.lead());
			newPassword2 = createLP(composite, repeatNewPassword.lead());

//			parent.pack();
			oldPassword.setFocus();
			return composite;
		}

		@Override
		protected void okPressed() {
			try {
				if (!newPassword1.getText().equals(newPassword2.getText()))
					throw new CmsException("New passwords are different");
				changePassword(oldPassword.getTextChars(), newPassword1.getTextChars());
				closeShell(OK);
			} catch (Exception e) {
				ErrorFeedback.show("Cannot change password", e);
			}
		}

		/** Creates label and password. */
		protected Text createLP(Composite parent, String label) {
			new Label(parent, SWT.NONE).setText(label);
			Text text = new Text(parent, SWT.SINGLE | SWT.LEAD | SWT.PASSWORD | SWT.BORDER);
			text.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			return text;
		}

	}

}
